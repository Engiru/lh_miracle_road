package dev.lhkongyu.lhmiracleroad.tool;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lhkongyu.lhmiracleroad.attributes.LHMiracleRoadAttributes;
import dev.lhkongyu.lhmiracleroad.capability.ItemStackPunishmentAttribute;
import dev.lhkongyu.lhmiracleroad.capability.ItemStackPunishmentAttributeProvider;
import dev.lhkongyu.lhmiracleroad.capability.PlayerOccupationAttribute;
import dev.lhkongyu.lhmiracleroad.config.LHMiracleRoadConfig;
import dev.lhkongyu.lhmiracleroad.data.reloader.PunishmentReloadListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.ItemAttributeModifierEvent;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

public class ItemPunishmentTool {

    /**
     * 物品的属性设置事件触发时，将能力里记录的物品属性直接注册进物品里
     * @param stack
     * @param event
     */
    public static void itemStackAddPunishmentAttribute(ItemStack stack, ItemAttributeModifierEvent event){
        stack.getCapability(ItemStackPunishmentAttributeProvider.ITEM_STACK_PUNISHMENT_ATTRIBUTE_PROVIDER).ifPresent(itemStackPunishmentAttribute -> {
            Map<Attribute, AttributeModifier> attributeModifierMap = itemStackPunishmentAttribute.getAttribute();
            for (Attribute attribute : attributeModifierMap.keySet()) {
                event.addModifier(attribute, attributeModifierMap.get(attribute));
            }
        });
    }

    /**
     * 物品惩罚能力注册
     * @param modifierMultimap
     * @param itemStackPunishmentAttribute
     * @param attribute
     * @param multiplier
     */
    public static void injectionItemStackPunishmentAttribute(Multimap<Attribute, AttributeModifier> modifierMultimap, ItemStackPunishmentAttribute itemStackPunishmentAttribute, Attribute attribute,double multiplier){
        if (modifierMultimap.containsKey(LHMiracleRoadAttributes.BURDEN)) return;
        modifierMultimap.forEach((key, value) -> {
            if (key.getDescriptionId().equals(attribute.getDescriptionId())) {
                itemStackPunishmentAttribute.setHeavy(value.getAmount() * multiplier);
            }
        });
        JsonArray attributeNeed = null;
        if (attribute.getDescriptionId().equals(Attributes.ATTACK_DAMAGE.getDescriptionId())) attributeNeed = setAttributeNeed(itemStackPunishmentAttribute.getHeavy());
        setHeavyAttributeModifier(itemStackPunishmentAttribute,attributeNeed);
    }

    /**
     * 给物品注册点数需求
     * @param itemStackPunishmentAttribute
     * @param attributeNeed
     */
    public static void setHeavyAttributeModifier(ItemStackPunishmentAttribute itemStackPunishmentAttribute,@Nullable JsonArray attributeNeed){
        AttributeModifier attributeModifier = new AttributeModifier(UUID.randomUUID(), "", itemStackPunishmentAttribute.getHeavy(), AttributeModifier.Operation.ADDITION);
        Map<String, AttributeModifier> map = Maps.newHashMap();
        map.put(NameTool.HEAVY,attributeModifier);
        itemStackPunishmentAttribute.setAttribute(map);
        itemStackPunishmentAttribute.setAttributeNeed(attributeNeed);
    }

    /**
     * 自定义点数需求
     * @param attackDamageAmount
     * @return
     */
    public static JsonArray setAttributeNeed(double attackDamageAmount){
        JsonArray jsonArray = new JsonArray();
        JsonObject attributeNeed = new JsonObject();
        attributeNeed.addProperty("attribute_id", "power");
        attributeNeed.addProperty("need_points", Integer.min((int) (attackDamageAmount * 1.25),80));
        jsonArray.add(attributeNeed);

        return jsonArray;
    }

    /**
     * 计算玩家把该物品穿上或拿在手上时的重量，根据重量进行属性上的奖惩
     * @param itemFrom
     * @param itemTo
     * @param itemFromPunishmentAttributeOptional
     * @param itemToPunishmentAttributeOptional
     * @param player
     * @param playerOccupationAttribute
     * @param slot
     */
    public static void heavyPunishmentAttributeModifier(ItemStack itemFrom,ItemStack itemTo,
                                                         Optional<ItemStackPunishmentAttribute> itemFromPunishmentAttributeOptional,
                                                         Optional<ItemStackPunishmentAttribute> itemToPunishmentAttributeOptional,
                                                         ServerPlayer player, PlayerOccupationAttribute playerOccupationAttribute,
                                                         EquipmentSlot slot){
        AttributeInstance heavyAttributeInstance = player.getAttribute(LHMiracleRoadAttributes.HEAVY);
        AttributeInstance burdenAttributeInstance = player.getAttribute(LHMiracleRoadAttributes.BURDEN);

        ItemStackPunishmentAttribute itemFromPunishmentAttribute = null;
        if (itemFromPunishmentAttributeOptional.isPresent() && !itemFrom.isEmpty()) itemFromPunishmentAttribute = itemFromPunishmentAttributeOptional.get();
        ItemStackPunishmentAttribute itemToPunishmentAttribute = null;
        if (itemToPunishmentAttributeOptional.isPresent() && !itemTo.isEmpty()) itemToPunishmentAttribute = itemToPunishmentAttributeOptional.get();

        double itemFromHeavy = itemFromPunishmentAttribute == null ? 0 : itemFromPunishmentAttribute.getHeavy();
        double itemToHeavy = itemToPunishmentAttribute == null ? 0 : itemToPunishmentAttribute.getHeavy();

        double heavy = heavyAttributeInstance.getValue();
        double heavyValue = 0;
        if (slot == EquipmentSlot.OFFHAND && LHMiracleRoadConfig.COMMON.IS_OFFHAND_CALCULATE_HEAVY.get()){
            heavyValue = heavy + itemToHeavy;
            playerOccupationAttribute.setOffhandHeavy(itemToHeavy);
        }
        else heavyValue = Double.max(heavy - itemFromHeavy,0) + itemToHeavy + playerOccupationAttribute.getOffhandHeavy();
        double burden = burdenAttributeInstance.getValue();

        setHeavyAttributeModifier(playerOccupationAttribute,player,heavyValue,burden);

    }

    /**
     * 给玩家设置重量的奖惩信息
     * @param playerOccupationAttribute
     * @param player
     * @param heavy
     * @param burden
     */
    public static void setHeavyAttributeModifier(PlayerOccupationAttribute playerOccupationAttribute, ServerPlayer player,double heavy,double burden){
        double proportion = (heavy / burden) * 100;
        AttributeModifier heavyAttributeModifier = playerOccupationAttribute.getHeavyAttributeModifier();
        if (heavyAttributeModifier != null){
            player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(heavyAttributeModifier);
            playerOccupationAttribute.setHeavyAttributeModifier(null);
        }
        AttributeModifier attributeModifier = null;
        UUID uuid = UUID.randomUUID();
        if (proportion > 100){
            attributeModifier = new AttributeModifier(uuid, "", LHMiracleRoadConfig.COMMON.PUNISHMENT_OVERWEIGHT.get(), AttributeModifier.Operation.MULTIPLY_BASE);
        }else if (proportion >= 75){
            attributeModifier = new AttributeModifier(uuid, "",  LHMiracleRoadConfig.COMMON.PUNISHMENT_BIASED_WEIGHT.get(), AttributeModifier.Operation.MULTIPLY_BASE);
        }else if (proportion >= 40){
            attributeModifier = new AttributeModifier(uuid, "", LHMiracleRoadConfig.COMMON.PUNISHMENT_NORMAL.get(), AttributeModifier.Operation.MULTIPLY_BASE);
        }else {
            attributeModifier = new AttributeModifier(uuid, "", LHMiracleRoadConfig.COMMON.PUNISHMENT_LIGHT.get(), AttributeModifier.Operation.MULTIPLY_BASE);
        }
        player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(attributeModifier);
        playerOccupationAttribute.setHeavyAttributeModifier(attributeModifier);
    }

    /**
     * 清除当前物品记录的惩罚能力和解除对玩家的惩罚
     * @param player
     * @param playerOccupationAttribute
     * @param itemFromPunishmentAttribute
     */
    public static void cleanItemFromPunishmentAttributeModifier(ServerPlayer player,PlayerOccupationAttribute playerOccupationAttribute,ItemStackPunishmentAttribute itemFromPunishmentAttribute){
        //判断是否启用点数限制
        if (!LHMiracleRoadConfig.COMMON.IS_SKILL_POINTS_RESTRICT.get()) return;
        Map<String, AttributeModifier> recordPunishmentAttributeModifier = itemFromPunishmentAttribute.getRecordPunishmentAttributeModifier();
        if (recordPunishmentAttributeModifier == null || recordPunishmentAttributeModifier.isEmpty()) return;

        recordPunishmentAttributeModifier.forEach((key,value)->{
            AttributeModifier attributeModifier = recordPunishmentAttributeModifier.get(key);
            Attribute attribute = LHMiracleRoadTool.stringConversionAttribute(key);
            AttributeInstance attributeInstance = player.getAttribute(attribute);
            if (attributeInstance != null){
                attributeInstance.removeModifier(attributeModifier);
                playerOccupationAttribute.removePunishmentAttributeModifier(attributeModifier.getId().toString());
            }
        });
        itemFromPunishmentAttribute.cleanRecordPunishmentAttributeModifier();
    }

    /**
     * 如果该物品存在能力判断玩家是否满足惩罚条件，如果满足就加上惩罚属性
     * @param player
     * @param playerOccupationAttribute
     * @param itemToPunishmentAttribute
     */
    public static void setItemToPunishmentAttributeModifier(ServerPlayer player,PlayerOccupationAttribute playerOccupationAttribute,ItemStackPunishmentAttribute itemToPunishmentAttribute){
        setItemToPunishmentAttributeModifier(player, playerOccupationAttribute, itemToPunishmentAttribute, null);
    }

    /**
     * If onApplied is provided, it will be invoked for every punishment applied with
     * the attribute name and the UUID of the modifier that was added to the player.
     */
    public static void setItemToPunishmentAttributeModifier(ServerPlayer player,
                                                            PlayerOccupationAttribute playerOccupationAttribute,
                                                            ItemStackPunishmentAttribute itemToPunishmentAttribute,
                                                            @Nullable BiConsumer<String, UUID> onApplied){
        //判断是否启用点数限制
        if (!LHMiracleRoadConfig.COMMON.IS_SKILL_POINTS_RESTRICT.get()) return;
        //判断是否启用点数限制
        Map<String, Integer> occupationAttributeLevel = playerOccupationAttribute.getOccupationAttributeLevel();
        JsonArray attributeNeed = itemToPunishmentAttribute.getAttributeNeed();
        if (attributeNeed == null || attributeNeed.isEmpty()) return;

        for (JsonElement jsonElement : attributeNeed) {
            JsonObject jsonObject = LHMiracleRoadTool.isAsJsonObject(jsonElement);
            if (jsonObject == null) continue;

            String attributeId = LHMiracleRoadTool.isAsString(jsonObject.get("attribute_id"));
            int needPoints = LHMiracleRoadTool.isAsInt(jsonObject.get("need_points"));
            Integer mapOccupationAttributeLevel = occupationAttributeLevel.get(attributeId);
            int mapCurioAttributeLevel = playerOccupationAttribute.getCurioAttributeLevelValue(attributeId);
            int attributeLevel = mapOccupationAttributeLevel == null ? 0 : mapOccupationAttributeLevel;
            attributeLevel += mapCurioAttributeLevel;

            if (attributeLevel < needPoints) {
                String punishmentId = LHMiracleRoadTool.isAsString(jsonObject.get("punishment_id"));
                if (punishmentId == null || punishmentId.isEmpty()) punishmentId = ResourceLocationTool.DEFAULT;
                JsonArray punishments = PunishmentReloadListener.DEFAULT_PUNISHMENT.get(punishmentId);
                if (punishments == null || punishments.isEmpty()) continue;

                for (JsonElement element : punishments) {
                    JsonObject punishment = LHMiracleRoadTool.isAsJsonObject(element);
                    String attributeName = LHMiracleRoadTool.isAsString(punishment.get("attribute"));
                    if (itemToPunishmentAttribute.getRecordPunishmentAttributeModifier().get(attributeName) != null) continue;
                    String operationName = LHMiracleRoadTool.isAsString(punishment.get("operation"));
                    double punishmentValue = LHMiracleRoadTool.isAsDouble(punishment.get("value"));
                    AttributeModifier.Operation operation = LHMiracleRoadTool.stringConversionOperation(operationName);
                    if (operation == null) continue;
                    Attribute attribute = LHMiracleRoadTool.stringConversionAttribute(attributeName);
                    if (attribute != null) {
                        // Before applying, ensure we don't stack duplicates for the same attribute: if already recorded, remove first
                        AttributeModifier existing = itemToPunishmentAttribute.getRecordPunishmentAttributeModifier().get(attributeName);
                        if (existing != null) {
                            // Best-effort removal on re-application path
                            AttributeInstance inst = player.getAttribute(attribute);
                            if (inst != null) inst.removeModifier(existing);
                        }
                        UUID uuid = UUID.randomUUID();
                        AttributeModifier attributeModifier = new AttributeModifier(uuid, "", punishmentValue, operation);
                        player.getAttribute(attribute).addTransientModifier(attributeModifier);
                        if (attribute.getDescriptionId().equals(Attributes.MAX_HEALTH.getDescriptionId())) {
                            player.setHealth((float) player.getAttribute(attribute).getValue());
                        }
                        playerOccupationAttribute.addPunishmentAttributeModifier(uuid.toString(), attributeModifier);
                        itemToPunishmentAttribute.addRecordPunishmentAttributeModifier(attributeName, attributeModifier);
                        if (onApplied != null) onApplied.accept(attributeName, uuid);
                    }
                }
                //如果有多个属性要求也只执行一次
                return;
            }
        }
    }
}
