package com.hackillinois2015.rumor;

/**
 * Created by dave on 2/28/15.
 */
public class CommonData {
    static boolean player;  //0 for Rumor; 1 for Truth
    static int skillPts;
    static int avgCrediPlayer;
    static int avgCrediAI;
    static int COLOR_RUMOR, COLOR_TRUTH;
    static int rx,ry;
    static GridState[][] state;
}


class GridState {
    int x, y;
    int flag;   // 0 for neutral; 1 for rumor; 2 for truth;
    int color;  // White for neutral; COLOR_RUMOR, COLOR_TRUTH
    boolean original;   // the first spreader
    int credibility;    // 谣言强度/公信力
    int spreadRate;     // 谣言扩展速度
    int spreadMethod;   //谣言传播方式 1. 近距离传播 2.新建立传播点 3. 舆论引导 4. 信息爆炸
    int rumorType;  //谣言属性（不同谣言政府会有不同的压制措施 1.无害谣言 2.Hot Topic 3.Controversial 4.伪科学谣言 5.trending

}