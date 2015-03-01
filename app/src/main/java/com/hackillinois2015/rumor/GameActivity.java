package com.hackillinois2015.rumor;

import android.graphics.Color;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Random;


public class GameActivity extends ActionBarActivity {

    Random random = new Random();
    long lastTime=0, total=0, timer=0;
    final int REFRESH_RATE = 10, NUM_BLOCK = 10, MAP_WIDTH = 611, MAP_HEIGHT=611;

    class GameUpdater implements Runnable {
        public void run() {
            long currTime = System.currentTimeMillis();
            total += currTime - lastTime;
            lastTime = currTime;
            timer += total;
            if (total/1000 == REFRESH_RATE){

                for (GridState[] s : CommonData.state){
                    for (GridState curr : s){
                        switch (curr.spreadMethod){
                            case 1:
                                localSpread(curr, curr.flag);
                                continue;
                            case 2:
                                opinionGuider(CommonData.rx, CommonData.ry, curr.flag);
                                continue;
                            case 3:
                                opinionGuider(CommonData.rx, CommonData.ry, curr.flag);
                                continue;
                            case 4:
                                infoExplosion(curr.flag);
                                continue;
                            default:
                                localSpread(curr, curr.flag);
                        }
                    }
                }
                if (timer % 60000 == 0) gainPts();
                updateColor();
                total = 0;
                gameView.postInvalidate();
            }
        }
        int[] direc(int r, int x, int y){
            switch (r){
                case 0:
                    return new int[]{x+1,y}; //shift right
                case 1:
                    return new int[]{x-1,y};//shift left
                case 2:
                    return new int[]{x,y+1};//left & down
                case 3:
                    return new int[]{x,y-1}; //right & up
                case 4:
                    return new int[]{x+1,y+1}; //right & down
                case 5:
                    return new int[]{x-1,y-1}; //left & up
                default:
                    return new int[]{x,y};
            }
        }
        void updateColor(){
            for (GridState[] s : CommonData.state){
                for (GridState curr : s){
                    if (curr.flag == 0)
                        curr.color = Color.WHITE;
                    else if (curr.flag == 1)
                        curr.color = CommonData.COLOR_RUMOR;
                    else curr.color = CommonData.COLOR_TRUTH;
                }
            }
        }
        void gainPts(){
            CommonData.skillPts++;
        }
        void localSpread(GridState currState, int flag){
            currState.flag = flag;
            for (int i = 0; i < currState.spreadRate; i++){
                int[] xy = direc(random.nextInt(6), currState.x, currState.y);    //随机扩展
                GridState nextState = CommonData.state[xy[0]][xy[1]];

                if (nextState.flag != 0){       //污染过程 非中立
                    if ((Math.min(currState.credibility, nextState.credibility) / 2) < Math.abs(currState.credibility - nextState.credibility)) {
                        if (currState.credibility > nextState.credibility){
                            nextState.flag = currState.flag;
                            currState.credibility += (int)(nextState.credibility*0.1);
                        }
                        else currState.flag = nextState.flag;
                        nextState.credibility = currState.credibility-1;
                    }
                    else{ nextState.credibility-=5; currState.credibility-=5;}
                }
                else if (nextState.flag == currState.flag){
                    currState.spreadRate = ((1+currState.spreadRate)>6)?6:currState.spreadRate++;
                }
                else{       //中立
                    int r = (int)(101*Math.random());
                    if (currState.credibility > r) {
                        nextState.flag = currState.flag;
                        nextState.credibility = currState.credibility-1;
                    }
                }
                if (currState.credibility<=0)currState.flag = Color.WHITE;
            }
            if (!currState.original){
                currState.credibility -= (currState.flag==CommonData.COLOR_TRUTH) ? 2 : 5;
            }
        }
        void attentionGetter(int amount, int flag){     //引起注意 增加近距离传播能力
            if (CommonData.skillPts == 0) return;
            for (GridState[] s : CommonData.state){
                for (GridState curr : s){
                    if (curr.flag == flag){
                        curr.spreadRate = ((curr.spreadRate+amount)>6)?6:((curr.spreadRate+amount)<0)?0:(curr.spreadRate+amount);
                    }
                }
            }
            CommonData.skillPts--;
        }
        void opinionGuider(int x, int y, int flag){    // 舆论引导 牺牲短时间内的行动力，可以定向传染一定的地块
            localSpread(CommonData.state[x][y], flag);
            attentionGetter(-1, flag);
        }
        void infoExplosion(int flag){   // 信息爆炸 永久牺牲行动力，可以一次性大规模扩展地块，公信力降低
            int x = random.nextInt(MAP_WIDTH);
            int y = random.nextInt(MAP_HEIGHT);
            for (int i = x; i < NUM_BLOCK; i++){
                for (int j = y; j < NUM_BLOCK; j++){
                    if (i < MAP_WIDTH && j < MAP_HEIGHT)
                        CommonData.state[i][j].flag = flag;
                }
            }
            attentionGetter(-6, flag);
        }
        void research(int amount, int flag){   //科学考证 增加公信力 在公信度低于对方平均值 可以使用技能
            for (GridState[] s : CommonData.state){
                for (GridState curr : s){
                    if (curr.flag == flag){
                        curr.credibility = ((curr.credibility+amount)>100)?100:(curr.credibility+amount);
                    }
                }
            }
        }
        void typeTransform(int flag){   //  信息转型 对自己的信息进行升级
            for (GridState[] s : CommonData.state){
                for (GridState curr : s){
                    if (curr.flag == flag){
                        curr.rumorType = (curr.rumorType>5)?5:(curr.rumorType+1);
                    }
                }
            }
        }
        void econInvest(int x, int y){ //  经济注资 提升大量特定一个区域的感染力 对于一个区域人口低于某个值 公信力高 拥有两个技能点 触发技能

        }
        void netSpread(int flag){// 网络推广 生成一个网络地块（不需要显示在地图上）。若网络模块有n个，每20秒，地图上有一定几率生成n个新的同类模块。生成的模块信用度降低

        }
    }

    Thread updaterThread = null;
    GameView gameView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameView = (GameView)findViewById(R.id.gameview);
        if(updaterThread == null)
            updaterThread = new Thread(new GameUpdater());
        try {
            updaterThread.start();
        } catch(IllegalThreadStateException err) {
            err.printStackTrace();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_game, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
