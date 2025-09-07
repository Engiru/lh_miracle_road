package dev.lhkongyu.lhmiracleroad.client.screen;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lhkongyu.lhmiracleroad.LHMiracleRoad;
import dev.lhkongyu.lhmiracleroad.data.ClientData;
import dev.lhkongyu.lhmiracleroad.data.reloader.OccupationReloadListener;
import dev.lhkongyu.lhmiracleroad.tool.LHMiracleRoadTool;
import dev.lhkongyu.lhmiracleroad.tool.ResourceLocationTool;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InitCoordinate {

    private final int pageY;

    private final int pageLeftX;

    private final int pageRightX;

    private final int pageWidth;

    private final int pageHeight;

    private final int frameX;

    private final int frameY;

    private final int frameWidth;

    private final int frameHeight;

    private int selectX;

    private int selectY;

    private int selectWidth;

    private int selectHeight;

    private MutableComponent selectComponent;

    private final int occupationX;

    private final int occupationY;

    private final int occupationWidth;

    private final int occupationHeight;

    private ResourceLocation occupationImage;

    private int initItemX;

    private int initItemY;

    private MutableComponent initItemComponent;

    private final int initAttributeX;

    private final int initAttributeY;

    private final MutableComponent initAttributeComponent;

    private final int describeOneLnInitX;

    private final int describeOtherLnInitX;

    private final int describeInitY;

    private List<String> describeTexts;

    private MutableComponent occupationNameComponent;

    private int occupationNameX;

    private int occupationNameY;

    private JsonObject occupation;

    private Map<String,Integer> initAttributeLevel;

    private List<JsonObject> initItem;

//    private final Map<String,Map<String,String>> attributePromoteValueShow = Maps.newHashMap();

    public int getPageY() {
        return pageY;
    }

    public int getPageLeftX() {
        return pageLeftX;
    }

    public int getPageRightX() {
        return pageRightX;
    }

    public int getPageWidth() {
        return pageWidth;
    }

    public int getPageHeight() {
        return pageHeight;
    }

    public int getFrameX() {
        return frameX;
    }

    public int getFrameY() {
        return frameY;
    }

    public int getFrameWidth() {
        return frameWidth;
    }

    public int getFrameHeight() {
        return frameHeight;
    }

    public int getSelectX() {
        return selectX;
    }

    public int getSelectY() {
        return selectY;
    }

    public int getSelectWidth() {
        return selectWidth;
    }

    public int getSelectHeight() {
        return selectHeight;
    }

    public int getOccupationX() {
        return occupationX;
    }

    public int getOccupationY() {
        return occupationY;
    }

    public int getOccupationWidth() {
        return occupationWidth;
    }

    public int getOccupationHeight() {
        return occupationHeight;
    }

    public int getInitItemX() {
        return initItemX;
    }

    public int getInitItemY() {
        return initItemY;
    }

    public int getInitAttributeX() {
        return initAttributeX;
    }

    public int getInitAttributeY() {
        return initAttributeY;
    }

    public int getDescribeOneLnInitX() {
        return describeOneLnInitX;
    }

    public int getDescribeOtherLnInitX() {
        return describeOtherLnInitX;
    }

    public int getDescribeInitY() {
        return describeInitY;
    }

    public List<String> getDescribeTexts() {
        return describeTexts;
    }

    public MutableComponent getSelectComponent() {
        return selectComponent;
    }

    public MutableComponent getInitItemComponent() {
        return initItemComponent;
    }

    public MutableComponent getInitAttributeComponent() {
        return initAttributeComponent;
    }

    public MutableComponent getOccupationNameComponent() {
        return occupationNameComponent;
    }

    public int getOccupationNameX() {
        return occupationNameX;
    }

    public int getOccupationNameY() {
        return occupationNameY;
    }

    public ResourceLocation getOccupationImage() {
        return occupationImage;
    }

    public JsonObject getOccupation() {
        return occupation;
    }

    public Map<String, Integer> getInitAttributeLevel() {
        return initAttributeLevel;
    }

    public List<JsonObject> getInitItem() {
        return initItem;
    }

    public InitCoordinate(int widthCore, int heightCore,int backgroundWidth,int backgroundHeight,Font font,int current){
        int lineHeight = font.lineHeight;
        int lineWidth = font.width("测试");
        int y = 5;

        //计算切换页数的位置
        pageWidth = 32;
        pageHeight = 32;
        pageY = heightCore + backgroundHeight - 52;
        pageLeftX = widthCore + pageWidth - 14;
        pageRightX = widthCore + backgroundWidth - 49;

        //计算职业相框的位置
        frameWidth = backgroundWidth / 3;
        frameHeight = (int) (backgroundHeight * 0.45 );
        frameX = widthCore + (frameWidth / 3);
        frameY = heightCore + y;
        //计算选择框的位置以及文本
        selectWidth = (int) (backgroundWidth * 0.125);
        selectHeight = (int) (backgroundHeight * 0.06 );
        selectX = widthCore + (backgroundWidth / 6 + selectWidth / 2);
        selectY = pageY + y * 2 + 3;
        selectComponent = Component.translatable("lhmiracleroad.gui.text.select");
        //计算职业图片的位置
        occupationWidth = (int) (backgroundWidth * 0.225);
        occupationHeight = (int) (backgroundHeight * 0.25 );
        occupationX = widthCore + (backgroundWidth / 6);
        occupationY = heightCore + (frameHeight / 5);
        // 计算初始物品的位置以及文本
        initItemX = widthCore + lineWidth * 2;
        initItemY = heightCore + (backgroundHeight / 2) + lineHeight * 3 + y;
        initItemComponent = Component.translatable("lhmiracleroad.gui.titles.init_item");
        // 计算初始属性的位置以及文本
        initAttributeX = widthCore + (backgroundWidth / 2) + lineWidth;
        initAttributeY = heightCore + lineHeight * 2;
        initAttributeComponent = Component.translatable("lhmiracleroad.gui.titles.init_attribute");
        //设置描述文本的位置
        describeOneLnInitX = (int) (widthCore + lineWidth * 2.5);
        describeOtherLnInitX = widthCore + lineWidth * 2;
        describeInitY = heightCore + frameHeight - lineHeight + y * 3;

        //设置职业基本数据
        setOccupation(widthCore,heightCore,backgroundWidth,backgroundHeight,font,current);
    }

    public InitCoordinate(int widthCore, int heightCore,int backgroundWidth,int backgroundHeight,Font font,String occupationId){
        int lineHeight = font.lineHeight;
        int lineWidth = font.width("测试");
        int y = 5;
        //计算切换页数的位置
        pageWidth = 32;
        pageHeight = 32;
        pageY = heightCore + backgroundHeight - 52;
        pageLeftX = widthCore + pageWidth - 14;
        pageRightX = widthCore + backgroundWidth - 49;

        //计算职业相框的位置
        frameWidth = backgroundWidth / 3;
        frameHeight = (int) (backgroundHeight * 0.5 );
        frameX = widthCore + (frameWidth / 3);
        frameY = heightCore + y;
        //计算职业图片的位置
        occupationWidth = (int) (backgroundWidth * 0.225);
        occupationHeight = (int) (backgroundHeight * 0.25 );
        occupationX = widthCore + (backgroundWidth / 6);
        occupationY = heightCore + (frameHeight / 5);
        // 计算初始属性的位置以及文本
        initAttributeX = widthCore + (backgroundWidth / 2) + lineWidth;
        initAttributeY = heightCore + lineHeight * 2;
        initAttributeComponent = Component.translatable("lhmiracleroad.gui.titles.init_attribute");
        //设置描述文本的位置
        describeOneLnInitX = (int) (widthCore + lineWidth * 2.5);
        describeOtherLnInitX = widthCore + lineWidth * 2;
        describeInitY = heightCore + frameHeight - lineHeight + y * 3;
        //选择框y
        selectWidth = (int) (backgroundWidth * 0.125);
        selectHeight = (int) (backgroundHeight * 0.06 );
        selectY = pageY + y * 2 + 3;
        //设置职业基本数据
        setOccupation(widthCore,heightCore,backgroundWidth,backgroundHeight,font,occupationId);
    }

    private List<String> setDescribeTexts(int backgroundWidth,Font font){
        int baseMaxWidth = (int) (backgroundWidth  * 0.385);
        //通过id获取职业描述
        String occupationDescribe = ResourceLocationTool.OCCUPATION_DESCRIBE_PREFIX + LHMiracleRoadTool.isAsString(occupation.get("id"));
        MutableComponent mutableComponent = Component.translatable(occupationDescribe);
        return LHMiracleRoadTool.baseTextWidthSplitText(font,mutableComponent,baseMaxWidth,describeOneLnInitX,describeOtherLnInitX);
    }

    public void setOccupation(int widthCore, int heightCore,int backgroundWidth,int backgroundHeight, Font font, int current){
        int lineHeight = font.lineHeight;
        //获取职业基本数据
        if (ClientData.OCCUPATION.isEmpty()) return;
        occupation = ClientData.OCCUPATION.get(current);

        //通过id获取图片位置
        String occupationImagePath = ResourceLocationTool.OCCUPATION_IMAGE_PREFIX + LHMiracleRoadTool.isAsString(occupation.get("id")) + ResourceLocationTool.OCCUPATION_IMAGE_SUFFIX;
        //设置职业图片
        occupationImage = new ResourceLocation(LHMiracleRoad.MODID, occupationImagePath);

        //通过id获取职业名称
        String occupationName = ResourceLocationTool.OCCUPATION_NAME_PREFIX + LHMiracleRoadTool.isAsString(occupation.get("id"));
        //设置职业名称
        occupationNameComponent = Component.translatable(occupationName);
        //设置职业名称的位置
        int textWidth = font.width(occupationNameComponent);
        occupationNameX = widthCore + (backgroundWidth / 6 + frameWidth / 3) - (textWidth / 2);
        occupationNameY = (int) (heightCore + frameHeight - lineHeight * 2 + 6);
        //填充初始属性等级数据
        initAttributeLevel = LHMiracleRoadTool.setInitAttributeLevelClient(occupation);

        //设置描述文本
        describeTexts = setDescribeTexts(backgroundWidth,font);

        //填充初始物品信息
        initItem = LHMiracleRoadTool.setInitItemClient(occupation);
    }

    public void setOccupation(int widthCore, int heightCore,int backgroundWidth,int backgroundHeight, Font font,String occupationId){
        int lineHeight = font.lineHeight;
        //获取职业基本数据
        occupation = LHMiracleRoadTool.getOccupationClient(occupationId);

        //通过id获取图片位置
        String occupationImagePath = ResourceLocationTool.OCCUPATION_IMAGE_PREFIX + LHMiracleRoadTool.isAsString(occupation.get("id")) + ResourceLocationTool.OCCUPATION_IMAGE_SUFFIX;
        //设置职业图片
        occupationImage = new ResourceLocation(LHMiracleRoad.MODID, occupationImagePath);

        //通过id获取职业名称
        String occupationName = ResourceLocationTool.OCCUPATION_NAME_PREFIX + LHMiracleRoadTool.isAsString(occupation.get("id"));
        //设置职业名称
        occupationNameComponent = Component.translatable(occupationName);
        //设置职业名称的位置
        int textWidth = font.width(occupationNameComponent);
        occupationNameX = widthCore + (backgroundWidth / 6 + frameWidth / 3) - (textWidth / 2);
        occupationNameY = (int) (heightCore + frameHeight - lineHeight * 2 + 6);

        //填充初始属性等级数据
        initAttributeLevel = LHMiracleRoadTool.setInitAttributeLevelClient(occupation);

        //设置描述文本
        describeTexts = setDescribeTexts(backgroundWidth,font);
    }
}
