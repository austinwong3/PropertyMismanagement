package hello;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.response.api.ApiTestResponse;

import hello.Player;
import java.util.*;

import javax.lang.model.util.ElementScanner6;

import java.io.*;



public class GameDriver {
    public int step = 0;
    public String channelId = "";
    public ArrayList<Player> players = new ArrayList<Player>();
    public int pmIndex = 0;
    public int turnCounter = 0;
    public Player pm;
    public int[] missionSize = new int[5];

    private Slack slack = Slack.getInstance();
    private String token = System.getenv("SLACK_BOT_TOKEN");
    private MethodsClient methods = slack.methods(token);
    

    public int getStep()
    {
        return step;
    }
    public void setStep(int num)
    {
        step = num;
    }

    public void resetGame()
    {
        players = new ArrayList<Player>();
        begin(channelId);
    }

    public void begin(String chan)
    {
        step = 1;
        System.out.println("gamebegins");

        channelId = chan;
        try{
        ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Welcome to Property MisManagement!"));
        }
        catch(Exception e)
        {
            System.out.println("oof begin");
        }

        optIn();
    }

    public void optIn()
    {
        try{
        ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Please input `opt-in` to join the party..."));
        }
        catch(Exception e)
        {
            System.out.println("oof opt-in");
        }
    }

    public void addPlayer(String id)
    {
        try{
        
        if(players.size() == 8)
        {
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Max Player Count Met. Either input `ready` to begin or `restart` to start over."));
        }
        else{
            Player p = new Player(id);
            players.add(p);
        }

        if(players.size() < 5)
        {
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text(players.size() + " players in party. " + (5-players.size()) + " more player(s) needed to start game."));
        }
        else if(players.size() == 8)
        {
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Max Player Count Met"));
        }
        }
        catch(Exception e)
        {
            System.out.println("oof addPlayer");
        }

    }

    public void startTurnPhase()
    {

        //checks for sufficient players
        if(players.size() < 5)
        {
            try{
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Insufficient players. " + (5-players.size()) + " more player(s) needed to start game."));
            }
            catch(Exception e)
            {
                System.out.println("oof startTurns");
            }
            return;
        }
        if(players.size() > 8)
        {
            try{
                ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
                .channel(channelId)
                .text("Too many player...restarting..."));
                resetGame();
            }
            catch(Exception e)
            {
                System.out.println("oof startTurns");
            }
            return;
        }

        //step should == 2 (Turns Phase)
        step++;
        setRoles();

        setMissionSize();

        turnPhase();
    }

    //picks roles for each player
    public void setRoles()
    {
        int playerNum = players.size();

        int[] five  = {3, 2};
        int[] six = {4, 2};
        int[] seven = {4, 3};
        int[] eight = {5 ,3};

        int[] roster = {};
        if(playerNum == 5)
        {
            roster = five;
        }
        if(playerNum == 6)
        {
            roster = six;
        }
        if(playerNum == 7)
        {
            roster = seven;
        }
        if(playerNum == 8)
        {
            roster = eight;
        }

        ArrayList<String> spyIds = new ArrayList<String>();
        ArrayList<String> devIds = new ArrayList<String>();
        playerNum--;
        Random gen = new Random();
        while((roster[0]>0 || roster[1]>0) && playerNum >= 0)
        {
            int roleInd = gen.nextInt(2);

            if(roster[roleInd] <= 0)
            {
                while(roleInd == 0 && playerNum >=0)
                {
                    players.get(playerNum).setRole(true);
                    spyIds.add(players.get(playerNum).getId());
                    playerNum--;
                }
            }
            else if(roleInd == 1 && roster[roleInd] <= 0)
            {
                while(playerNum >=0)
                {
                    players.get(playerNum).setRole(false);
                    devIds.add(players.get(playerNum).getId());
                    playerNum--;
                }
            }
            else if(roleInd == 0)
            {
                players.get(playerNum).setRole(false);
                devIds.add(players.get(playerNum).getId());
                roster[roleInd]--;
                playerNum--;
            }
            else if(roleInd == 1)
            {
                players.get(playerNum).setRole(true);
                spyIds.add(players.get(playerNum).getId());
                roster[roleInd]--;
                playerNum--;
            }
        }
        System.out.println("Roles Set");

        contactSpies(spyIds);
        contactDevs(devIds);

        printRoles();
    }

    public void printRoles()
    {
        int counter = 0;
        while(counter < players.size())
        {
            System.out.println(players.get(counter).isSpy());
            counter++;
        }
    }

    public void contactSpies(ArrayList<String> spyIds)
    {
        String msg = "You are a spy!\nThe other spies are: ";
        int counter = 0;
        while(counter < spyIds.size())
        {
            msg+= "<@"+spyIds.get(counter)+"> ";
            counter++;
        }
        msg+= "\nYour job is to inject bugs without getting caught! Best of luck!";

        String finalMsg = msg;
        counter = 0;
        while(counter < spyIds.size())
        {
            String chan = spyIds.get(counter);
            try{
                ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
                .channel(chan)
                .text(finalMsg));
            }
            catch(Exception e)
            {
                System.out.println("oof contactSpies");
            }
            counter++;
        }
    }

    public void contactDevs(ArrayList<String> devIds)
    {

        int counter = 0;
        while(counter < devIds.size())
        {
            String chan = devIds.get(counter);
            try{
                ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
                .channel(chan)
                .text("You are a dev! Make your QAs proud and push some flawless PRs!"));
            }
            catch(Exception e)
            {
                System.out.println("oof contactDevs");
            }
            counter++;
        }
    }

    //sets size of team for each respective mission
    public void setMissionSize()
    {
        int playerNum = players.size();

        int[] five  = {2, 3, 2, 3, 3};
        int[] six = { 2, 3, 4, 3, 4};
        int[] seven = {2, 3, 3, 4, 4};
        int[] eight = {3, 4, 4, 5, 5};


        if(playerNum == 5)
        {
            missionSize = five;
        }
        if(playerNum == 6)
        {
            missionSize = six;
        }
        if(playerNum == 7)
        {
            missionSize = seven;
        }
        if(playerNum == 8)
        {
            missionSize = eight;
        }
    }

    public void turnPhase(){
        try{
            pm = players.get(pmIndex);
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Turn " + (turnCounter+1) + " begins.\n" +
                "<@" + pm.getId() + "> is the President, please choose a team of " + missionSize[turnCounter] + "." ));
        }
        catch(Exception e)
        {
            System.out.println("oof turnPhase");
        }
    }

}