package dev.lhkongyu.lhmiracleroad.screen;

import com.google.gson.JsonObject;
import dev.lhkongyu.lhmiracleroad.config.LHMiracleRoadConfig;
import dev.lhkongyu.lhmiracleroad.data.ClientData;
import dev.lhkongyu.lhmiracleroad.data.reloader.AttributeReloadListener;
import dev.lhkongyu.lhmiracleroad.data.reloader.OccupationReloadListener;
import dev.lhkongyu.lhmiracleroad.packet.PlayerAttributeChannel;
import dev.lhkongyu.lhmiracleroad.packet.PlayerOccupationMessage;
import dev.lhkongyu.lhmiracleroad.tool.LHMiracleRoadTool;
import dev.lhkongyu.lhmiracleroad.tool.ResourceLocationTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Stream;

public class LHMiracleRoadOccupationScreen extends Screen {

    private InitCoordinate initCoordinate;

    private int current;

    private List<Component> itemHoverTooltip = null;

    private int itemHoverTooltipX = 0;

    private int itemHoverTooltipY = 0;

    private List<Component> detailsHoverTooltip = null;

    private int detailsHoverTooltipX = 0;

    private int detailsHoverTooltipY = 0;

    private final int backgroundWidth = 450;

    private final int backgroundHeight = 245;

    private int widthCore;

    private int heightCore;

    private final int color = 0x000;


    public LHMiracleRoadOccupationScreen() {
        super(Component.empty());
        super.minecraft = Minecraft.getInstance();
        this.current = 0;
    }

    @Override
    public void init() {
        if (minecraft != null) {
            widthCore = (super.width - backgroundWidth) / 2;
            heightCore = (super.height - backgroundHeight) / 2;

            initCoordinate = new InitCoordinate(widthCore,heightCore,backgroundWidth,backgroundHeight,font,current);
            if (current == 0) showButton(false,true);
            else showButton(true, current != ClientData.OCCUPATION.size() - 1);
        }
    }

    @Override
    public void tick() {
        textFields().forEach(EditBox::tick);
    }

    private Stream<EditBox> textFields() {
        return children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics); // 渲染背景
        super.render(graphics, mouseX, mouseY, partialTick);
        // 渲染文字
        showTitle(graphics);
        showOccupationName(graphics);
        showDescribe(graphics);
        showOccupation(graphics);
        showAttribute(graphics,mouseX,mouseY);
        showItem(graphics,mouseX,mouseY);

        if (itemHoverTooltip != null) {
            graphics.renderComponentTooltip(font, itemHoverTooltip, itemHoverTooltipX, itemHoverTooltipY);
        }

        if (detailsHoverTooltip != null) {
            graphics.renderComponentTooltip(font,detailsHoverTooltip,detailsHoverTooltipX,detailsHoverTooltipY);
        }
    }

    private void lastPage() {
        if (current > 0) {
            current--;
            this.clearWidgets();
            if (current == 0){
                showButton(false,true);
            }else {
                showButton(true,true);
            }
            initCoordinate.setOccupation(widthCore,heightCore,backgroundWidth,backgroundHeight, super.font, current);
        }
    }

    private void nextPage() {
        if (current < ClientData.OCCUPATION.size() - 1) {
            current++;
            this.clearWidgets();
            if (current == ClientData.OCCUPATION.size() - 1){
                showButton(true,false);
            }else {
                showButton(true,true);
            }
            initCoordinate.setOccupation(widthCore,heightCore,backgroundWidth,backgroundHeight, super.font, current);
        }
    }

    private void select(){
        PlayerAttributeChannel.sendToServer(new PlayerOccupationMessage(LHMiracleRoadTool.isAsString(initCoordinate.getOccupation().get("id"))));
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        super.renderBackground(graphics);
        //背景图
        graphics.blit(ResourceLocationTool.Gui.background, widthCore, heightCore, 0, 0F, 0F, backgroundWidth, backgroundHeight, backgroundWidth, backgroundHeight);
        graphics.blit(ResourceLocationTool.Gui.frame, initCoordinate.getFrameX(), initCoordinate.getFrameY(), 0, 0, initCoordinate.getFrameWidth(),initCoordinate.getFrameHeight(),
                initCoordinate.getFrameWidth(),  initCoordinate.getFrameHeight());

        int width = (int) (initCoordinate.getSelectWidth() * 1.25);
        int height = initCoordinate.getSelectHeight();
        int lineHeight = font.lineHeight / 3;
        int lineWidth = font.width("测试") / 4;
        int titleItemX = initCoordinate.getInitItemX() - lineWidth;
        int titleItemY = initCoordinate.getInitItemY() - lineHeight;
        int titleAttributeX = initCoordinate.getInitAttributeX() - lineWidth;
        int titleAttributeY = initCoordinate.getInitAttributeY() - lineHeight;

        graphics.blit(ResourceLocationTool.Gui.title,titleItemX, titleItemY,0,0,width, height,
                width, height);

        graphics.blit(ResourceLocationTool.Gui.title,titleAttributeX, titleAttributeY,0,0,width, height,
                width, height);
    }

    private void showTitle(GuiGraphics graphics){
        graphics.drawString(font, initCoordinate.getInitItemComponent(), initCoordinate.getInitItemX(), initCoordinate.getInitItemY(), 0Xffffff, false);
        graphics.drawString(font, initCoordinate.getInitAttributeComponent(), initCoordinate.getInitAttributeX(), initCoordinate.getInitAttributeY(), 0Xffffff, false);
    }

    private void showOccupationName(GuiGraphics graphics){
        int x = initCoordinate.getOccupationNameX();
        int y = initCoordinate.getOccupationNameY();

        graphics.drawString(font, initCoordinate.getOccupationNameComponent(), x, y, color, false);

//        Minecraft minecraft = Minecraft.getInstance();
//        boolean isFullscreen = minecraft.getWindow().isFullscreen();
//        final int targetGuiScale = 4;
//        //适配缩放设置为4
//        if (currentGuiScale < targetGuiScale && currentGuiScale > 0 && isFullscreen) {
//            float scale = targetGuiScale / (float) currentGuiScale;
//            graphics.pose().pushPose();
//            graphics.pose().scale(scale, scale, 1);
//            graphics.drawString(font, initCoordinate.getOccupationNameComponent(), (int) (x / scale) - (2 * (targetGuiScale - currentGuiScale)), (int) (y / scale), 0x000000, false);
//
//            graphics.pose().popPose();
//        }else {
//            graphics.drawString(font, initCoordinate.getOccupationNameComponent(), x, y, 0x000000, false);
//        }
    }

    private void showDescribe(GuiGraphics graphics){
        int startY = initCoordinate.getDescribeInitY();
        List<String> lines = initCoordinate.getDescribeTexts();
        for (int i = 0; i < lines.size(); i++) {
            if (i == 0){
                graphics.drawString(font, Component.literal(lines.get(i)), initCoordinate.getDescribeOneLnInitX(), startY, color, false);
            }else {
                graphics.drawString(font, Component.literal(lines.get(i)), initCoordinate.getDescribeOtherLnInitX(), startY, color, false);
            }
            startY += font.lineHeight;
        }
    }

    private void showOccupation(GuiGraphics graphics){
        graphics.blit(initCoordinate.getOccupationImage(),  initCoordinate.getOccupationX(), initCoordinate.getOccupationY(), 0F, 0F,
                initCoordinate.getOccupationWidth(), initCoordinate.getOccupationHeight(), initCoordinate.getOccupationWidth(), initCoordinate.getOccupationHeight());
    }

    private void showAttribute(GuiGraphics graphics,int mouseX, int mouseY){
        int lineHeight = font.lineHeight;
        detailsHoverTooltip = null;
        detailsHoverTooltipX = 0;
        detailsHoverTooltipY = 0;
        int initNameTextX = initCoordinate.getInitAttributeX();
        int initAttributeLevelX = (int) (initNameTextX + (backgroundWidth * 0.175));
        Component details = Component.translatable("lhmiracleroad.gui.attribute.text.details");
        int textWidth = font.width(details);
        int initDetailsX =(int) (initNameTextX + (backgroundWidth * 0.175) * 1.75);
        int initY = initCoordinate.getInitAttributeY() + (lineHeight * 2);
        int initLevel = 0;
        int attributeSize = 0;
        for (String key : ClientData.ATTRIBUTE_TYPES.keySet()) {
            String nameText = ResourceLocationTool.ATTRIBUTE_NAME_PREFIX + key;
            int level = initCoordinate.getInitAttributeLevel().get(key);
            graphics.drawString(font, Component.translatable(nameText), initNameTextX, initY, color, false);
            graphics.drawString(font, String.valueOf(level), initAttributeLevelX, initY, color, false);
            graphics.drawString(font, details, initDetailsX, initY, 0xC56F49, false);

            // 检查鼠标是否悬停在物品上
            if (mouseX >= initDetailsX && mouseX <= initDetailsX + textWidth && mouseY >= initY && mouseY <= initY + lineHeight) {
                List<Component> components = LHMiracleRoadTool.getDescribeText(LHMiracleRoadTool.isAsJsonArray(ClientData.ATTRIBUTE_TYPES.get(key)),level,key);
                detailsHoverTooltip =  components;
                detailsHoverTooltipX = mouseX;
                detailsHoverTooltipY = mouseY;
            }

            initY += (int) (lineHeight * 1.75);
            initLevel += level;
            attributeSize ++;
        }

        initLevel = (initLevel - attributeSize * LHMiracleRoadConfig.COMMON.LEVEL_BASE.get()) + 1;
        Component initLevelcomponent = Component.translatable("lhmiracleroad.gui.attribute.text.init_level",initLevel);
        Component difficultyLevelcomponent = Component.translatable("lhmiracleroad.gui.attribute.text.difficulty_level",LHMiracleRoadTool.isAsInt(initCoordinate.getOccupation().get("init_difficulty_level")));
        String initLevelText = initLevelcomponent.getString();
        String difficultyLevelText = difficultyLevelcomponent.getString();
        String levelText = initLevelText+" "+difficultyLevelText;
        textWidth = font.width(levelText);
        graphics.drawString(font, levelText, initAttributeLevelX - (textWidth / 2), initCoordinate.getSelectY(), color, false);
    }

    private void showItem(GuiGraphics graphics, int mouseX, int mouseY){
        int lineHeight = font.lineHeight;
        int lineWidth = 22;
        itemHoverTooltip = null;
        itemHoverTooltipX = 0;
        itemHoverTooltipY = 0;
        int itemSize = 24;
        int x = initCoordinate.getInitItemX();
        int y = (int) (initCoordinate.getInitItemY() + lineHeight * 1.45);
        int spacing = 0;
        int lineSpacing = 0;
        int quantityBreak = 8; //每循环多少次物品时换行

        for (int i = 0; i < initCoordinate.getInitItem().size(); i++) {
            JsonObject object = initCoordinate.getInitItem().get(i);
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(LHMiracleRoadTool.isAsString(object.get("item"))));
            if (item == null) continue;
            int quantity = LHMiracleRoadTool.isAsInt(object.get("quantity"));
            ItemStack itemStack = new ItemStack(item, quantity);
            if (itemStack.isEmpty()) continue;
            String tag = LHMiracleRoadTool.isAsString(object.get("tag"));
            LHMiracleRoadTool.setTag(itemStack,tag);
            graphics.renderItem(itemStack, x + spacing, y + lineSpacing);
            graphics.renderItemDecorations(font, itemStack, x + spacing, y + lineSpacing, String.valueOf(quantity));

            // 检查鼠标是否悬停在物品上
            if (mouseX >= x + spacing && mouseX <= x + spacing + itemSize && mouseY >= y + lineSpacing && mouseY <= y + lineSpacing + itemSize) {
                TooltipFlag tooltipFlag = minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
                itemHoverTooltip = itemStack.getTooltipLines(null, tooltipFlag);
                itemHoverTooltipX = mouseX;
                itemHoverTooltipY = mouseY;
            }

            spacing += lineWidth;
            if ((i + 1) % quantityBreak == 0){
                spacing = 0;
                lineSpacing += (int) (lineHeight * 2.5);
            }
        }

    }

    private void showButton(boolean leftShow,boolean rightShow){
        //选择按钮
        ImageButton showSelectButton =
                new ImageButton(initCoordinate.getSelectX(), initCoordinate.getSelectY(), initCoordinate.getSelectWidth(), initCoordinate.getSelectHeight(), initCoordinate.getSelectComponent(),
                        true,true,ResourceLocationTool.Gui.select,ResourceLocationTool.Gui.selectButton,0,0,initCoordinate.getSelectWidth(),initCoordinate.getSelectHeight(),
                        initCoordinate.getSelectWidth(),initCoordinate.getSelectHeight());
        showSelectButton.setPressFunc(b -> select());
        addRenderableWidget(showSelectButton);

        if (leftShow) {
            //切换页数按钮
            ImageButton showPageLeftButton =
                    new ImageButton(initCoordinate.getPageLeftX(), initCoordinate.getPageY(), initCoordinate.getPageWidth(), initCoordinate.getPageHeight(), Component.empty(),
                            true, false, ResourceLocationTool.Gui.pageLeft, ResourceLocationTool.Gui.pageLeftButton, 0, 0, initCoordinate.getPageWidth(), initCoordinate.getPageHeight(),
                            initCoordinate.getPageWidth(), initCoordinate.getPageHeight());
            showPageLeftButton.setPressFunc(b -> lastPage());
            addRenderableWidget(showPageLeftButton);
        }

        if (rightShow) {
            ImageButton showPageRightButton =
                    new ImageButton(initCoordinate.getPageRightX(), initCoordinate.getPageY(), initCoordinate.getPageWidth(), initCoordinate.getPageHeight(), Component.empty(),
                            true, false, ResourceLocationTool.Gui.pageRight, ResourceLocationTool.Gui.pageRightButton, 0, 0, initCoordinate.getPageWidth(), initCoordinate.getPageHeight(),
                            initCoordinate.getPageWidth(), initCoordinate.getPageHeight());
            showPageRightButton.setPressFunc(b -> nextPage());
            addRenderableWidget(showPageRightButton);
        }
    }
}
