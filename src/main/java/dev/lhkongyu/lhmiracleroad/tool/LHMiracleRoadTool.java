package dev.lhkongyu.lhmiracleroad.tool;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.lhkongyu.lhmiracleroad.LHMiracleRoad;
import dev.lhkongyu.lhmiracleroad.attributes.AttributeInstanceAccess;
import dev.lhkongyu.lhmiracleroad.attributes.LHMiracleRoadAttributes;
import dev.lhkongyu.lhmiracleroad.attributes.ShowAttributesTypes;
import dev.lhkongyu.lhmiracleroad.capability.ItemStackPunishmentAttribute;
import dev.lhkongyu.lhmiracleroad.capability.ItemStackPunishmentAttributeProvider;
import dev.lhkongyu.lhmiracleroad.capability.PlayerOccupationAttribute;
import dev.lhkongyu.lhmiracleroad.config.LHMiracleRoadConfig;
import dev.lhkongyu.lhmiracleroad.data.ClientData;
import dev.lhkongyu.lhmiracleroad.data.reloader.*;
import dev.lhkongyu.lhmiracleroad.entity.player.PlayerSoulEntity;
import dev.lhkongyu.lhmiracleroad.event.InteractionEvent;
import dev.lhkongyu.lhmiracleroad.generator.SpellDamageTypes;
import dev.lhkongyu.lhmiracleroad.packet.ClientDataMessage;
import dev.lhkongyu.lhmiracleroad.packet.ClientOccupationMessage;
import dev.lhkongyu.lhmiracleroad.packet.ClientSoulMessage;
import dev.lhkongyu.lhmiracleroad.packet.PlayerChannel;
import dev.lhkongyu.lhmiracleroad.data.reloader.EquipmentReloadListener;
import dev.lhkongyu.lhmiracleroad.client.particle.SoulParticleOption;
import dev.lhkongyu.lhmiracleroad.registry.ItemsRegistry;
import dev.lhkongyu.lhmiracleroad.tool.mathcalculator.MathCalculatorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.ModList;
import org.joml.Math;
import org.joml.Vector3f;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import net.minecraftforge.registries.ForgeRegistries;
// Curios API (optional at runtime; compile-time available in this project)
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

public class LHMiracleRoadTool {

    public static final Map<UUID, PlayerSoulEntity> SOUL_ENTITY_MAP = new HashMap<>();

    public static RandomSource randomSource = RandomSource.create();

    // Cache of last-seen Curios signatures per player for cheap change detection
    public static final Map<UUID, String> CURIOS_SIGNATURES = new HashMap<>();
    // Per-player tick counters for periodic Curios sweeps
    public static final Map<UUID, Integer> CURIOS_TICK = new HashMap<>();

    // Fixed UUID for Curios heavy aggregation modifier
    private static final UUID CURIOS_HEAVY_UUID = UUID.nameUUIDFromBytes("lhmiracleroad:curios_heavy".getBytes());

    // Track Curios-origin punishment modifier UUIDs by player with the attribute name they were applied to
    // Map<PlayerUUID, Map<ModifierUUID, AttributeName>>
    private static final Map<UUID, Map<UUID, String>> CURIOS_PUNISHMENTS = new HashMap<>();

    /**
     * 转入路径转换为 带 modid 的 ResourceLocation对象
     * @param path
     * @return
     */
    public static ResourceLocation resourceLocationId(String path){

        return new ResourceLocation(LHMiracleRoad.MODID,path);
    }

    /**
     * rgb 转换 为 Vector3f
     * @param red
     * @param green
     * @param blue
     * @return
     */
    public static Vector3f RGBChangeVector3f(int red, int green, int blue){
        // 将通道值归一化到范围[0, 1]
        float normalizedRed = (float) red / 255.0f;
        float normalizedGreen = (float) green / 255.0f;
        float normalizedBlue = (float) blue / 255.0f;

        return new Vector3f(normalizedRed, normalizedGreen, normalizedBlue);
    }

    /**
     * 将秒 转换为 tick
     * @param duration
     * @return
     */
    public static int getDuration(int duration){
        return duration * 20;
    }

    /**
     * 根据文本的宽度来进行拆分文本
     *
     * @param font             mc文本字体
     * @param mutableComponent 渲染的文本
     * @param baseMaxWidth     文字每行的最大宽度基础数值
     * @param oneLnInitX       文字第一行开头的x轴位置
     * @param otherLnInitX     文字除第一行开头的x轴位置
     * @return
     */
    public static List<String> baseTextWidthSplitText(Font font, MutableComponent mutableComponent, int baseMaxWidth, int oneLnInitX, int otherLnInitX) {
        String text = mutableComponent.getString();
        List<String> lines = new ArrayList<>();
        int maxWidth = baseMaxWidth; // 最大宽度
        int currentWidth = 0;
        StringBuilder currentLine = new StringBuilder();

        for (char c : text.toCharArray()) {
            currentWidth += font.width(Character.toString(c));
            if (currentWidth > maxWidth) {
                maxWidth = baseMaxWidth + (oneLnInitX - otherLnInitX);
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
                currentWidth = font.width(Character.toString(c));
            }
            currentLine.append(c);
        }
        lines.add(currentLine.toString());
        return lines;
    }

    /**
     * 获取描述文本
     *
     * @param jsonArray
     * @param level
     * @param id
     * @return
     */
    public static List<Component> getDescribeText(JsonArray jsonArray, int level, String id) {
        List<Component> components = new ArrayList<>();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject object = jsonElement.getAsJsonObject();
            String modId = isAsString(object.get("condition"));
            String describe = ResourceLocationTool.ATTRIBUTE_DETAILS_TEXT_PREFIX + isAsString(object.get("describe"));
            String attribute = isAsString(object.get("attribute"));
            int percentageBase = 100;
            if (modId != null && !modId.isEmpty()) {
                if (isModExist(modId))
                    components.add(Component.translatable(describe, getDescribeTextValue(level, id, attribute,percentageBase)));
            } else
                components.add(Component.translatable(describe, getDescribeTextValue(level, id, attribute,percentageBase)));
        }
        return components;
    }

    /**
     * 计算出描述文本后显示的值
     *
     * @param level
     * @param id
     * @param attribute
     * @return
     */
    private static String getDescribeTextValue(int level, String id, String attribute,int percentageBase) {
        String showValue = null;
//        Map<String, String> valueMap = attributePromoteValueShow.get(id);
//        if (valueMap != null) {
//            showValue = valueMap.get(attribute);
//            if (showValue != null) return showValue;
//        }
        JsonObject data = ClientData.ATTRIBUTE_POINTS_REWARDS.get(id);
        if (data == null) return "";
        JsonArray pointsRewards = isAsJsonArray(data.get("points_rewards"));
        if (pointsRewards == null || pointsRewards.isEmpty()) return "";
        for (JsonElement jsonElement : pointsRewards) {
            JsonObject jsonObject = isAsJsonObject(jsonElement);
            String rewardsAttribute = isAsString(jsonObject.get("attribute"));
            if (rewardsAttribute == null) return "";
            if (attribute.equals(rewardsAttribute)) {
                double value = isAsDouble(jsonObject.get("value"));
                int levelPromote = isAsInt(jsonObject.get("level_promote"));
                double levelPromoteValue = isAsDouble(jsonObject.get("level_promote_value"));
                AttributeModifier.Operation operation = stringConversionOperation(isAsString(jsonObject.get("operation")));
                if (operation == null) return "";
                double min = LHMiracleRoadTool.isAsDouble(jsonObject.get("min"));
                double attributeValue = LHMiracleRoadTool.calculateTotalIncrease(level, value, levelPromoteValue, levelPromote,min);
                showValue = switch (operation) {
                    case ADDITION -> "+ " + attributeValue;
                    case MULTIPLY_BASE, MULTIPLY_TOTAL -> "+ " + new BigDecimal(attributeValue * percentageBase).setScale(4, RoundingMode.HALF_UP).doubleValue() + "%";
                };
//                Map<String, String> map = Maps.newHashMap();
//                map.put(attribute, showValue);
//                attributePromoteValueShow.put(id, map);
                return showValue;
            }

        }
        return "";
    }

    public static boolean isJsonArrayModIdsExist(JsonArray jsonArray) {
        if (jsonArray == null || jsonArray.isEmpty()) return false;
        for (int i = 0; i < jsonArray.size(); i++) {
            if (isModExist(jsonArray.get(i).getAsString())) return false;
        }
        return true;
    }

    /**
     * 获取该mod是否存在
     *
     * @param modId
     * @return
     */
    public static boolean isModExist(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static String isAsString(JsonElement jsonElement) {
        return jsonElement != null ? jsonElement.getAsString() : null;
    }

    public static JsonArray isAsJsonArray(JsonElement jsonElement) {
        return jsonElement != null ? jsonElement.getAsJsonArray() : new JsonArray();
    }

    public static JsonObject isAsJsonObject(JsonElement jsonElement) {
        return jsonElement != null ? jsonElement.getAsJsonObject() : null;
    }

    public static int isAsInt(JsonElement jsonElement) {
        return jsonElement != null ? jsonElement.getAsInt() : 0;
    }

    public static double isAsDouble(JsonElement jsonElement) {
        return jsonElement != null ? jsonElement.getAsDouble() : 0.0;
    }

    public static Boolean isAsBoolean(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.getAsBoolean();
    }

    /**
     * 计算出该属性当前等级所提升的值
     *
     * @param level
     * @param value
     * @param levelPromoteValue
     * @param levelPromote
     * @return
     */
    public static double calculateTotalIncrease(int level, double value, double levelPromoteValue, int levelPromote,double min) {
        level = level - LHMiracleRoadConfig.COMMON.LEVEL_BASE.get();
        if (level == 0) return 0.0;
        double returnValue = 0.0;
        BigDecimal dfReturnValue = null;
        if (levelPromote > 0) {
            // 根据当前等级计算提升次数 也就是组
            int twenties = level / levelPromote;

            if (levelPromoteValue < 0) {
                levelPromoteValue = levelPromoteValue > value ? value - min : levelPromoteValue;
            }
            double increase = 0;
            if (twenties > 0) {
                double levelIncrease = value * twenties * levelPromote;
//                increase = levelIncrease + ((double) (twenties * (twenties - 1)) / 2) * levelPromote * levelPromoteValue;
                double decreasingValue = 0;
                for (int i = 0;i < twenties;i++){
                    double thisRoundPromoteValue = levelPromoteValue * i;
                    decreasingValue += levelPromote * (value + thisRoundPromoteValue < min ? (value - min) * -1 : thisRoundPromoteValue);
                }
                increase = levelIncrease + decreasingValue;
            }
            // 用于计算 当前等级不满足一组的部分
            int remainingLevels = level % levelPromote;
            double especiallyDecreasing = levelPromoteValue * twenties + value;
            double increaseTheRest = Math.max(especiallyDecreasing,min) * remainingLevels;

            returnValue = increase + increaseTheRest;
            dfReturnValue = new BigDecimal(returnValue);
            return dfReturnValue.setScale(4, RoundingMode.HALF_UP).doubleValue();
        }
        returnValue = value * level;
        dfReturnValue = new BigDecimal(returnValue);
        return dfReturnValue.setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 通过职业信息获取，职业每个属性的初始等级
     *
     * @param occupation
     * @return
     */
    public static Map<String, Integer> setInitAttributeLevel(JsonObject occupation) {
        Map<String, Integer> map = Maps.newHashMap();
        JsonArray initAttribute = LHMiracleRoadTool.isAsJsonArray(occupation.get("init_attribute"));
        for (JsonElement jsonElement : initAttribute) {
            JsonObject object = LHMiracleRoadTool.isAsJsonObject(jsonElement);
            if (object == null) continue;
            String id = object.get("id").getAsString();
            JsonArray attributeObject = AttributeReloadListener.ATTRIBUTE_TYPES.get(id);
            if (attributeObject == null || attributeObject.isEmpty()) continue;
            int level = object.get("level").getAsInt();
            map.put(id, level);
        }

        return map;
    }

    /**
     * 通过职业信息获取，职业每个属性的初始等级(客户端)
     * @param occupation
     * @return
     */
    public static Map<String, Integer> setInitAttributeLevelClient(JsonObject occupation) {
        Map<String, Integer> map = Maps.newHashMap();
        JsonArray initAttribute = LHMiracleRoadTool.isAsJsonArray(occupation.get("init_attribute"));
        for (JsonElement jsonElement : initAttribute) {
            JsonObject object = LHMiracleRoadTool.isAsJsonObject(jsonElement);
            if (object == null) continue;
            String id = object.get("id").getAsString();
            JsonArray attributeObject = ClientData.ATTRIBUTE_TYPES.get(id);
            if (attributeObject == null || attributeObject.isEmpty()) continue;
            int level = object.get("level").getAsInt();
            map.put(id, level);
        }

        return map;
    }

    /**
     * 通过职业信息获取该职业初始物品信息
     *
     * @param occupation
     * @return
     */
    public static List<JsonObject> setInitItem(JsonObject occupation) {
        List<JsonObject> objects = new ArrayList<>();
        JsonArray initItem = InitItemResourceReloadListener.INIT_ITEM.get(isAsString(occupation.get("id")));
        if (initItem == null || initItem.isEmpty()) return objects;
        for (JsonElement jsonElement : initItem) {
            JsonObject object = LHMiracleRoadTool.isAsJsonObject(jsonElement);
            if (object == null) continue;
            objects.add(object);
        }
        return objects;
    }

    /**
     * 通过职业信息获取该职业客户端的初始物品信息
     *
     * @param occupation
     * @return
     */
    public static List<JsonObject> setInitItemClient(JsonObject occupation) {
        List<JsonObject> objects = new ArrayList<>();
        JsonArray initItem = ClientData.INIT_ITEM.get(isAsString(occupation.get("id")));
        if (initItem == null || initItem.isEmpty()) return objects;
        for (JsonElement jsonElement : initItem) {
            JsonObject object = LHMiracleRoadTool.isAsJsonObject(jsonElement);
            if (object == null) continue;
            objects.add(object);
        }
        return objects;
    }

    /**
     * 设置始物品标签
     *
     * @param itemStack
     * @param tagObj
     */
    public static void setTag(ItemStack itemStack, String tagObj) {
        if (tagObj != null) {
            CompoundTag modTag;
            try {
                modTag = NbtUtils.snbtToStructure(tagObj);
                modTag.remove("palette");
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
            itemStack.setTag(modTag);
        }
    }

    /**
     * 将string转换成 根据基础属性增加值(addition)还是 根据基础属性百分比提升（multiply_base）或是 根据总属性按百分比提升(multiply_total)
     *
     * @param operationString
     * @return
     */
    public static AttributeModifier.Operation stringConversionOperation(String operationString) {
        if (operationString == null) return null;
        return switch (operationString) {
            case "addition" -> AttributeModifier.Operation.ADDITION;
            case "multiply_base" -> AttributeModifier.Operation.MULTIPLY_BASE;
            case "multiply_total" -> AttributeModifier.Operation.MULTIPLY_TOTAL;
            default -> null;
        };
    }

    /**
     * 将属性名称转换成属性对象
     *
     * @param attributeName
     * @return
     */
    public static Attribute stringConversionAttribute(String attributeName) {
        if (attributeName == null) return null;
        int index = attributeName.indexOf('#');
        if (index != -1) {
            attributeName = attributeName.substring(0, index).trim();
        }

        return switch (attributeName) {
            case NameTool.MAX_HEALTH -> Attributes.MAX_HEALTH;
            case NameTool.ATTACK_DAMAGE -> Attributes.ATTACK_DAMAGE;
            case NameTool.RANGED_DAMAGE -> LHMiracleRoadAttributes.RANGED_DAMAGE;
            case NameTool.BURDEN -> LHMiracleRoadAttributes.BURDEN;
            case NameTool.HEAVY -> LHMiracleRoadAttributes.HEAVY;
            case NameTool.ARMOR -> Attributes.ARMOR;
            case NameTool.ARMOR_TOUGHNESS -> Attributes.ARMOR_TOUGHNESS;
            case NameTool.ATTACK_SPEED -> Attributes.ATTACK_SPEED;
            case NameTool.MOVEMENT_SPEED -> Attributes.MOVEMENT_SPEED;
            case NameTool.LUCK -> Attributes.LUCK;
            case NameTool.HEALING -> LHMiracleRoadAttributes.HEALING;
            case NameTool.HUNGER -> LHMiracleRoadAttributes.HUNGER;
            case NameTool.JUMP -> LHMiracleRoadAttributes.JUMP;
            case NameTool.CRITICAL_HIT_RATE -> LHMiracleRoadAttributes.CRITICAL_HIT_RATE;
            case NameTool.CRITICAL_HIT_DAMAGE -> LHMiracleRoadAttributes.CRITICAL_HIT_DAMAGE;
            case NameTool.DAMAGE_REDUCTION -> LHMiracleRoadAttributes.DAMAGE_REDUCTION;
            case NameTool.SOUL_INCREASE -> LHMiracleRoadAttributes.SOUL_INCREASE;
            case NameTool.DAMAGE_ADDITION -> LHMiracleRoadAttributes.DAMAGE_ADDITION;
            case NameTool.MINING_SPEED -> LHMiracleRoadAttributes.MINING_SPEED;
            case NameTool.MAGIC_DAMAGE_ADDITION -> LHMiracleRoadAttributes.MAGIC_DAMAGE_ADDITION;
            case NameTool.MAGIC_ATTRIBUTE_DAMAGE -> LHMiracleRoadAttributes.MAGIC_ATTRIBUTE_DAMAGE;
            case NameTool.FLAME_ATTRIBUTE_DAMAGE -> LHMiracleRoadAttributes.FLAME_ATTRIBUTE_DAMAGE;
            case NameTool.LIGHTNING_ATTRIBUTE_DAMAGE -> LHMiracleRoadAttributes.LIGHTNING_ATTRIBUTE_DAMAGE;
            case NameTool.DARK_ATTRIBUTE_DAMAGE -> LHMiracleRoadAttributes.DARK_ATTRIBUTE_DAMAGE;
            case NameTool.HOLY_ATTRIBUTE_DAMAGE -> LHMiracleRoadAttributes.HOLY_ATTRIBUTE_DAMAGE;
            case NameTool.ATTACK_CONVERT_MAGIC -> LHMiracleRoadAttributes.ATTACK_CONVERT_MAGIC;
            case NameTool.ATTACK_CONVERT_FLAME -> LHMiracleRoadAttributes.ATTACK_CONVERT_FLAME;
            case NameTool.ATTACK_CONVERT_LIGHTNING -> LHMiracleRoadAttributes.ATTACK_CONVERT_LIGHTNING;
            case NameTool.ATTACK_CONVERT_DARK -> LHMiracleRoadAttributes.ATTACK_CONVERT_DARK;
            case NameTool.ATTACK_CONVERT_HOLY -> LHMiracleRoadAttributes.ATTACK_CONVERT_HOLY;
            default -> {
                Attribute attr = AttributePointsRewardsReloadListener.recordAttribute.get(attributeName);
                if (attr != null) yield attr;
                // Fallback: try to resolve from registry using full resource location (e.g., irons_spellbooks:spell_power)
                try {
                    ResourceLocation rl = new ResourceLocation(attributeName);
                    Attribute regAttr = ForgeRegistries.ATTRIBUTES.getValue(rl);
                    yield regAttr;
                } catch (Exception ignored) {
                    yield null;
                }
            }
        };
    }

    /**
     * 判断该属性名称是否符合属性对象的 descriptionId
     *
     * @param descriptionId
     * @param attributeName
     * @return
     */
    public static boolean isAttributeName(String descriptionId, String attributeName) {
        String[] parts = attributeName.split(":");
        if (parts.length != 2) {
            // 如果不符合key:value的格式，则直接返回false
            return false;
        }
        String key = parts[0];
        String value = parts[1];
        if (descriptionId.equals(value)) {
            return true;
        }

        String format = key + "." + value;
        if (descriptionId.equals(format)) {
            return true;
        }
        return descriptionId.equals("attribute.name." + format) || descriptionId.equals("attribute." + format);
    }

    /**
     * 设置部分属性基础值
     *
     * @param player
     * @param initDifficultyLevel
     */
    public static void setConfigBaseAttribute(ServerPlayer player, int initDifficultyLevel) {
        player.getAttribute(LHMiracleRoadAttributes.BURDEN).setBaseValue(LHMiracleRoadConfig.COMMON.INIT_BURDEN.get());
        player.getAttribute(LHMiracleRoadAttributes.INIT_DIFFICULTY_LEVEL).setBaseValue(initDifficultyLevel);
    }

    /**
     * 将服务端信息同步至客户端信息中
     *
     * @param playerOccupationAttribute
     * @param player
     */
    public static void synchronizationClient(PlayerOccupationAttribute playerOccupationAttribute, ServerPlayer player) {
        AttributeInstance burden = player.getAttribute(LHMiracleRoadAttributes.BURDEN);
        int burdenValue = 0;
        if (burden != null) {
            burdenValue = (int) burden.getValue();
        }
        UUID playerUUID = player.getUUID();
        playerOccupationAttribute.setBurden(burdenValue);
        JsonObject playerOccupationAttributeObject = playerOccupationAttribute.getPlayerOccupationAttribute(playerUUID);
        ClientOccupationMessage message = new ClientOccupationMessage(playerOccupationAttributeObject);
        PlayerChannel.sendToClient(message, player);
    }

    //同步显示的数据信息
    public static void synchronizationShowAttribute(ServerPlayer player){
        //同步显示的数据信息
        JsonObject showAttributeData = new JsonObject();
        JsonObject showAttribute = setShowAttribute(player);
        showAttributeData.addProperty("key","showAttribute");
        showAttributeData.add("data",showAttribute);

        ClientDataMessage attributeTypesMessage = new ClientDataMessage(showAttributeData);
        PlayerChannel.sendToClient(attributeTypesMessage, player);
    }

    /**
     * 同步获取的灵魂
     * @param occupationExperience
     * @param player
     * @param soulStart
     */
    public static void synchronizationSoul(Integer occupationExperience, ServerPlayer player,Integer soulStart) {
        UUID playerUUID = player.getUUID();
        JsonObject playerOccupationAttributeObject = new JsonObject();
        playerOccupationAttributeObject.addProperty("playerUUID",playerUUID.toString());
        playerOccupationAttributeObject.addProperty("occupationExperience",occupationExperience);
        playerOccupationAttributeObject.addProperty("soulStart",soulStart);
        ClientSoulMessage message = new ClientSoulMessage(playerOccupationAttributeObject);
        PlayerChannel.sendToClient(message, player);
    }

    /**
     * 解析并计算字符串公式
     *
     * @param formula
     * @param level
     * @return
     */
    public static int evaluateFormula(String formula, int level) {
        formula = formula.replaceAll("lv", String.valueOf(level));
        return MathCalculatorUtil.getCalculatorInt(formula);
    }

    /**
     * 解析并计算字符串公式
     * @param formula
     * @param exp
     * @param hp
     * @param atk
     * @param arm
     * @param atou
     * @param buff
     * @return
     */
    public static int evaluateFormula(String formula,int exp, int hp,int atk,int arm,int atou,int buff) {
        formula = formula.replaceAll("exp", String.valueOf(exp));
        formula = formula.replaceAll("hp", String.valueOf(hp));
        formula = formula.replaceAll("atk", String.valueOf(atk));
        formula = formula.replaceAll("arm", String.valueOf(arm));
        formula = formula.replaceAll("atou", String.valueOf(atou));
        formula = formula.replaceAll("buff", String.valueOf(buff));
        return MathCalculatorUtil.getCalculatorInt(formula);
    }

    /**
     * 设置在gui显示的属性信息
     *
     * @param player
     * @return
     */
    public static JsonObject setShowAttribute(ServerPlayer player) {
        JsonObject showAttribute =  new JsonObject();
        for (JsonObject attributeObject : ShowGuiAttributeReloadListener.SHOW_GUI_ATTRIBUTE) {
            String attributeName = LHMiracleRoadTool.isAsString(attributeObject.get("attribute"));
            ShowAttributesTypes showValueType = ShowAttributesTypes.fromString(LHMiracleRoadTool.isAsString(attributeObject.get("show_value_type")));

            Attribute attribute = stringConversionAttribute(attributeName);
            if (attribute == null) continue;
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance == null) continue;

//            showAttributeObject.addProperty("v", attributeInstance.getValue());
//            showAttributeObject.addProperty("b", attributeInstance.getBaseValue());
            double modifierValue = 0.0;
            AttributeInstanceAccess attributeInstanceAccess = ((AttributeInstanceAccess) attributeInstance);
            switch (showValueType){
                case BASE,BASE_PERCENTAGE:
                    if (attributeInstance.getBaseValue() > 0){
                        modifierValue = attributeInstanceAccess.computeIncreasedValueForInitial(0);
                        break;
                    }
                case EXTRA_BASE,EXTRA_PERCENTAGE:
                    modifierValue = attributeInstanceAccess.computeIncreasedValueForInitial(attributeInstance.getBaseValue() > 0 ? 0 : 1);
                    if (attributeName.equals(NameTool.CRITICAL_HIT_RATE) && modifierValue == 100) {
                        modifierValue = attributeInstanceAccess.computeIncreasedValueForInitial(0);
                        break;
                    }
                    modifierValue -= attributeInstance.getBaseValue() > 0 ? attributeInstance.getBaseValue() : 1;
                    break;
            };
            showAttribute.addProperty(attributeName, modifierValue);
        }
        return showAttribute;
    }

    /**
     * 概率计算
     *
     * @param probability
     * @return
     */
    public static boolean percentageProbability(int probability) {
        if (probability >= 100) return true;
        else if (probability < 1) return false;

        return new Random().nextInt(100) < probability;
    }

    /**
     * 概率计算
     *
     * @param probability
     * @return
     */
    public static boolean percentageProbability(double probability) {
        if (probability >= 100.0) return true;
        else if (probability <= 0.0) return false;

        return new Random().nextDouble() * 100.0 < probability;
    }

    /**
     * 玩家的奖惩状态更新
     *
     * @param player
     */
    public static void playerPunishmentStateUpdate(ServerPlayer player, PlayerOccupationAttribute playerOccupationAttribute) {
        //更新一下重量的奖惩状态
        AttributeInstance heavyAttributeInstance = player.getAttribute(LHMiracleRoadAttributes.HEAVY);
        AttributeInstance burdenAttributeInstance = player.getAttribute(LHMiracleRoadAttributes.BURDEN);
        double heavy = heavyAttributeInstance.getValue();
        double burden = burdenAttributeInstance.getValue();
        ItemPunishmentTool.setHeavyAttributeModifier(playerOccupationAttribute, player, heavy, burden);

        customEquipmentUpdate(player, playerOccupationAttribute, EquipmentSlot.HEAD, null);
        customEquipmentUpdate(player, playerOccupationAttribute, EquipmentSlot.CHEST, null);
        customEquipmentUpdate(player, playerOccupationAttribute, EquipmentSlot.LEGS, null);
        customEquipmentUpdate(player, playerOccupationAttribute, EquipmentSlot.FEET, null);
        customEquipmentUpdate(player, playerOccupationAttribute, null, InteractionHand.MAIN_HAND);
        customEquipmentUpdate(player, playerOccupationAttribute, null, InteractionHand.OFF_HAND);

    // Also process Curios slots (e.g., ISB spellbook) if Curios is loaded
    updateCuriosEquipment(player, playerOccupationAttribute);
    }

    /**
     * 自定义某个位置的装备 并对他进行更新操作
     *
     * @param player
     * @param playerOccupationAttribute
     * @param equipmentSlot
     */
    private static void customEquipmentUpdate(ServerPlayer player, PlayerOccupationAttribute playerOccupationAttribute, EquipmentSlot equipmentSlot, InteractionHand interactionHand) {
        ItemStack stack = null;
        if (equipmentSlot != null) stack = player.getItemBySlot(equipmentSlot);
        else if (interactionHand != null) stack = player.getItemInHand(interactionHand);
        else return;

        if (stack.isEmpty()) return;
        Optional<ItemStackPunishmentAttribute> itemStackPunishmentAttribute = stack
                .getCapability(ItemStackPunishmentAttributeProvider.ITEM_STACK_PUNISHMENT_ATTRIBUTE_PROVIDER)
                .resolve();
        if (itemStackPunishmentAttribute.isEmpty()) return;
        //先清空惩罚信息
        ItemPunishmentTool.cleanItemFromPunishmentAttributeModifier(player, playerOccupationAttribute, itemStackPunishmentAttribute.get());
        //然后重新计算设置一下惩罚信息
        ItemPunishmentTool.setItemToPunishmentAttributeModifier(player, playerOccupationAttribute, itemStackPunishmentAttribute.get());
    }

    /**
     * Iterate Curios slots and update punishment states for any stacks that carry our capability.
     * Safe no-op if Curios mod isn't present or no Curios inventory exists for player.
     */
    public static void updateCuriosEquipment(ServerPlayer player, PlayerOccupationAttribute playerOccupationAttribute) {
        if (!isModExist("curios")) return;
        var opt = CuriosApi.getCuriosInventory(player).resolve();
        if (opt.isEmpty()) return;
        ICuriosItemHandler curios = opt.get();
        Map<String, ICurioStacksHandler> curiosMap = curios.getCurios();
        if (curiosMap == null || curiosMap.isEmpty()) return;
        // 1) Sum Curios-provided heavy and apply a single transient modifier on HEAVY
    double curiosHeavy = 0.0;
    // collect modifiers applied during this sweep: uuid -> attributeName
    Map<UUID, String> appliedThisSweep = new HashMap<>();
        for (Map.Entry<String, ICurioStacksHandler> entry : curiosMap.entrySet()) {
            ICurioStacksHandler handler = entry.getValue();
            if (handler == null) continue;
            var stacks = handler.getStacks();
            if (stacks == null) continue;
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack == null || stack.isEmpty()) continue;
                Optional<ItemStackPunishmentAttribute> itemAttr = stack
                        .getCapability(ItemStackPunishmentAttributeProvider.ITEM_STACK_PUNISHMENT_ATTRIBUTE_PROVIDER)
                        .resolve();
                if (itemAttr.isEmpty()) {
            // Fallback: populate from data pack mapping if available (helps items missing cap attach)
            JsonObject equip = getEquipment(EquipmentReloadListener.EQUIPMENT, stack.getItem().getDescriptionId());
            var key = ForgeRegistries.ITEMS.getKey(stack.getItem());
                    if (equip != null) {
                        // Note: we cannot attach a new capability here, but log for clarity
            LHMiracleRoad.LOGGER.debug("[CuriosSweep] Missing cap for {} (descId={}), has equipment data -> please re-create stack or ensure cap attaches via AttachCapabilitiesEvent.",
                key, stack.getItem().getDescriptionId());
                    } else {
            LHMiracleRoad.LOGGER.debug("[CuriosSweep] No cap and no equipment for {} (descId={})", key, stack.getItem().getDescriptionId());
                    }
                    continue;
                }
                // If capability exists but lacks attribute_need, try hydrate from equipment map
                if (itemAttr.get().getAttributeNeed() == null || itemAttr.get().getAttributeNeed().isEmpty()) {
                    JsonObject equip = getEquipment(EquipmentReloadListener.EQUIPMENT, stack.getItem().getDescriptionId());
                    if (equip != null) {
                        int hv = isAsInt(equip.get(NameTool.HEAVY));
                        JsonArray need = isAsJsonArray(equip.get("attribute_need"));
                        itemAttr.get().setHeavy(hv);
                        ItemPunishmentTool.setHeavyAttributeModifier(itemAttr.get(), need);
                        LHMiracleRoad.LOGGER.debug("[CuriosSweep] Hydrated cap for {} from equipment data (needs={})", ForgeRegistries.ITEMS.getKey(stack.getItem()), need.size());
                    }
                }
                // Sum heavy
                curiosHeavy += itemAttr.get().getHeavy();
                // Clean and re-apply punishment specific to this stack; record applied UUIDs
                ItemPunishmentTool.cleanItemFromPunishmentAttributeModifier(player, playerOccupationAttribute, itemAttr.get());
                ItemPunishmentTool.setItemToPunishmentAttributeModifier(player, playerOccupationAttribute, itemAttr.get(),
                    (attrName, uuid) -> appliedThisSweep.put(uuid, attrName)
                );
                LHMiracleRoad.LOGGER.trace("[CuriosSweep] Applied punishments (if any) for {}", ForgeRegistries.ITEMS.getKey(stack.getItem()));
            }
        }
        // Remove any previously applied Curios-origin punishments that did not get re-applied this sweep
        Map<UUID, String> previously = CURIOS_PUNISHMENTS.getOrDefault(player.getUUID(), Collections.emptyMap());
        if (!previously.isEmpty()) {
            for (Map.Entry<UUID, String> entry : previously.entrySet()) {
                UUID uuid = entry.getKey();
                String attrName = entry.getValue();
                if (!appliedThisSweep.containsKey(uuid)) {
                    AttributeModifier mod = playerOccupationAttribute.getPunishmentAttributeModifier().get(uuid.toString());
                    if (mod != null) {
                        Attribute attr = stringConversionAttribute(attrName);
                        if (attr != null) {
                            AttributeInstance inst = player.getAttribute(attr);
                            if (inst != null) inst.removeModifier(uuid);
                        } else {
                            // Fallback: sweep all attributes if resolution failed
                            for (Attribute a : ForgeRegistries.ATTRIBUTES.getValues()) {
                                AttributeInstance inst = player.getAttribute(a);
                                if (inst != null && inst.getModifier(uuid) != null) inst.removeModifier(uuid);
                            }
                        }
                        playerOccupationAttribute.removePunishmentAttributeModifier(uuid.toString());
                    }
                }
            }
        }
        CURIOS_PUNISHMENTS.put(player.getUUID(), appliedThisSweep);
        // Apply aggregated heavy modifier
        AttributeInstance heavyAttr = player.getAttribute(LHMiracleRoadAttributes.HEAVY);
        if (heavyAttr != null) {
            // remove existing curios-heavy modifier if present
            if (heavyAttr.getModifier(CURIOS_HEAVY_UUID) != null) {
                heavyAttr.removeModifier(CURIOS_HEAVY_UUID);
            }
            if (curiosHeavy != 0.0) {
                AttributeModifier modifier = new AttributeModifier(CURIOS_HEAVY_UUID, "curios_heavy", curiosHeavy, AttributeModifier.Operation.ADDITION);
                heavyAttr.addTransientModifier(modifier);
            }
        }
        // Recompute movement penalty from heavy after updating HEAVY value
        AttributeInstance heavyAttributeInstance = player.getAttribute(LHMiracleRoadAttributes.HEAVY);
        AttributeInstance burdenAttributeInstance = player.getAttribute(LHMiracleRoadAttributes.BURDEN);
        if (heavyAttributeInstance != null && burdenAttributeInstance != null) {
            ItemPunishmentTool.setHeavyAttributeModifier(playerOccupationAttribute, player, heavyAttributeInstance.getValue(), burdenAttributeInstance.getValue());
        }
    }

    /**
     * Build a compact signature of the player's Curios inventory to detect changes.
     * Returns empty if Curios not loaded or no inventory.
     */
    public static Optional<String> getCuriosSignature(ServerPlayer player) {
        if (!isModExist("curios")) return Optional.empty();
        var opt = CuriosApi.getCuriosInventory(player).resolve();
        if (opt.isEmpty()) return Optional.empty();
        ICuriosItemHandler curios = opt.get();
        Map<String, ICurioStacksHandler> curiosMap = curios.getCurios();
        if (curiosMap == null || curiosMap.isEmpty()) return Optional.of("");
        StringBuilder sb = new StringBuilder();
        curiosMap.forEach((slotId, handler) -> {
            if (handler == null) return;
            var stacks = handler.getStacks();
            if (stacks == null) return;
            sb.append(slotId).append(':');
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (stack == null || stack.isEmpty()) {
                    sb.append("|");
                } else {
                    sb.append(stack.getItem().toString());
                    // include basic tag hash if present to detect NBT changes
                    if (stack.hasTag()) sb.append('#').append(Objects.hashCode(stack.getTag()));
                    sb.append('|');
                }
            }
            sb.append(';');
        });
        return Optional.of(sb.toString());
    }

    /**
     * True every N ticks for this player (server-only), used as a safety net to re-apply Curios punishments.
     */
    public static boolean curiosPeriodicTick(ServerPlayer player, int intervalTicks) {
        UUID id = player.getUUID();
        int n = CURIOS_TICK.getOrDefault(id, 0) + 1;
        if (n >= intervalTicks) {
            CURIOS_TICK.put(id, 0);
            return true;
        }
        CURIOS_TICK.put(id, n);
        return false;
    }

    /**
     * 通过职业id获取职业信息
     *
     * @param occupationId
     * @return
     */
    public static JsonObject getOccupation(String occupationId) {
        JsonObject occupation = null;
        for (int i = 0; i < OccupationReloadListener.OCCUPATION.size(); i++) {
            JsonObject jsonObject = OccupationReloadListener.OCCUPATION.get(i);
            if (jsonObject.get("id").getAsString().equals(occupationId)) {
                occupation = jsonObject;
            }
        }
        return occupation;
    }

    public static JsonObject getOccupationClient(String occupationId) {
        JsonObject occupation = null;
        for (int i = 0; i < ClientData.OCCUPATION.size(); i++) {
            JsonObject jsonObject = ClientData.OCCUPATION.get(i);
            if (jsonObject.get("id").getAsString().equals(occupationId)) {
                occupation = jsonObject;
            }
        }
        return occupation;
    }

    /**
     * 显示的value格式
     * @return
     */
    public static String getShowValueType(ShowAttributesTypes showValueType,double modifierValue,int percentageBase){
        final double v = new BigDecimal(modifierValue).setScale(4, RoundingMode.HALF_UP).doubleValue();
        final BigDecimal bigDecimal = new BigDecimal(modifierValue * percentageBase).setScale(4, RoundingMode.HALF_UP);
        return switch (showValueType){
            case BASE -> String.valueOf(v);
            case EXTRA_BASE -> "+" + v;
            case BASE_PERCENTAGE -> bigDecimal.doubleValue() + "%";
            case EXTRA_PERCENTAGE -> "+" + bigDecimal.doubleValue() + "%";
        };
    }

    public static boolean isShowPointsButton(int currentLevel,int maxLevel,int attributeMaxLevel){
        if (attributeMaxLevel < 1) return currentLevel < maxLevel;
        else return currentLevel < attributeMaxLevel;
    }

    public static JsonObject getEquipment(Map<String,Map<String,JsonObject>> equipment,String id){
        int lastIndex = id.lastIndexOf(".");
        if (lastIndex == -1) return null;
        String firstPart = id.substring(0, lastIndex);
        String secondPart = id.substring(lastIndex + 1);
        if (equipment.get(firstPart) == null) return null;
        return equipment.get(firstPart).get(secondPart);
    }

    public static void addItemStack(ServerPlayer player,ItemStack itemStack){
        boolean wasAdded = player.getInventory().add(itemStack);
        if (!wasAdded) {
            player.drop(itemStack, false);
        }
    }

    public static void getSoulParticle(ServerLevel serverLevel, ServerPlayer player, int soulCount,int max){
        int particleCount = Math.min(Math.max(soulCount / 200,10),max);
        float speed = .1f + ((float) particleCount / max * .025f);
        serverLevel.sendParticles(player,new SoulParticleOption(player.getId()), true, player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(), particleCount, 0.1, 0.1, 0.1,speed);
    }

    public static void getSoulParticle(ServerLevel serverLevel, ServerPlayer player, int soulCount, int max,int soulCountDivisor, Entity target){
        if (target == null) return;
        int particleCount = Math.min(Math.max(soulCount / soulCountDivisor,10),max);
        float speed = .1f + ((float) particleCount / max * .025f);
        serverLevel.sendParticles(player,new SoulParticleOption(player.getId()), true, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), particleCount, 0.1, 0.1, 0.1,speed);
    }

    public static void getSoulParticle(ServerLevel serverLevel, ServerPlayer player, int soulCount, int max,int min,int soulCountDivisor, Entity target){
        if (target == null) return;
        int particleCount = Math.min(Math.max(soulCount / soulCountDivisor,min),max);
        float speed = .1f + ((float) particleCount / max * .025f);
        serverLevel.sendParticles(player,new SoulParticleOption(player.getId()), true, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), particleCount, 0.1, 0.1, 0.1,speed);
    }

    public static double getAttributeValue(AttributeInstance attributeInstance){
        if (attributeInstance != null) return attributeInstance.getValue();
        return 0;
    }

    public static Queue<Integer> getIntegerSequence(int start,int end){
        Queue<Integer> queue = new LinkedList<>();
        int steps = 150;
        int intervals = steps - 1; // 间隔数

        int difference = end - start;

        int step = difference / intervals;

        for (int i = 1; i < steps - 1; i++) {
            int value = start + i * step;
            queue.add(value);
            if (value == end) return queue;
        }
        queue.add(end);

        return queue;
    }

    public static String getGemType(ItemStack itemStack){
        if (itemStack.getDescriptionId().equals(ItemsRegistry.FLAME_GEM.get().getDescriptionId())){
            return NameTool.FLAME;
        }else if (itemStack.getDescriptionId().equals(ItemsRegistry.LIGHTNING_GEM.get().getDescriptionId())){
            return NameTool.LIGHTNING;
        }else if (itemStack.getDescriptionId().equals(ItemsRegistry.DARK_GEM.get().getDescriptionId())){
            return NameTool.DARK;
        }else if (itemStack.getDescriptionId().equals(ItemsRegistry.BLOOD_GEM.get().getDescriptionId())){
            return NameTool.BLOOD;
        }else if (itemStack.getDescriptionId().equals(ItemsRegistry.MAGIC_GEM.get().getDescriptionId())){
            return NameTool.MAGIC;
        }else if (itemStack.getDescriptionId().equals(ItemsRegistry.SHARP_GEM.get().getDescriptionId())){
            return NameTool.SHARP;
        }else if (itemStack.getDescriptionId().equals(ItemsRegistry.HOLY_GEM.get().getDescriptionId())){
            return NameTool.HOLY;
        }else if (itemStack.getDescriptionId().equals(ItemsRegistry.ICE_GEM.get().getDescriptionId())){
            return NameTool.ICE;
        }else if (itemStack.getDescriptionId().equals(ItemsRegistry.POISON_GEM.get().getDescriptionId())){
            return NameTool.POISON;
        }else return null;
    }

    public static boolean itemIsWeaponsAll(ItemStack left){
        return itemIsWeapons(left) || itemIsRangedWeapons(left);
    }

    public static boolean itemIsWeapons(ItemStack left){
        return left.is(InteractionEvent.WEAPONS)
                || left.getItem() instanceof SwordItem
                || left.getItem() instanceof AxeItem
                || left.getItem() instanceof TridentItem;
    }

    public static boolean itemIsRangedWeapons(ItemStack left){
        return left.is(InteractionEvent.RANGED_WEAPONS) || left.getItem() instanceof ProjectileWeaponItem;
    }

//    /**
//     * 获取造成的伤害类型 并且无视护甲
//     * @param entity
//     * @param resourceKey
//     * @return
//     */
//    public static DamageSource getDamageSourceType(Entity entity, ResourceKey<DamageType> resourceKey,TagKey<DamageType> tagKey){
//        Set<TagKey<DamageType>> tagSet = new HashSet<>();
//        tagSet.add(tagKey);
//        return getDamageSource(entity,resourceKey,tagSet);
//    }

    /**
     * 获取攻击类型
     * @param entity
     * @return
     */
    public static DamageSource getDamageSource(Entity entity, ResourceKey<DamageType> key) {
        return entity.level().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolder(key)
                .<DamageSource>map(holder -> new DamageSource(holder, entity))
                .orElse(entity.level().damageSources().generic());
    }

    public static boolean isMagicDamage(DamageSource damageSource){
        if (damageSource.is(SpellDamageTypes.FLAME_MAGIC)){
            return true;
        }else if (damageSource.is(SpellDamageTypes.HOLY_MAGIC)){
            return true;
        }else if (damageSource.is(SpellDamageTypes.DARK_MAGIC)){
            return true;
        }else if (damageSource.is(SpellDamageTypes.LIGHTNING_MAGIC)){
            return true;
        }else return damageSource.is(SpellDamageTypes.MAGIC);
    }
}
