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
    public ArrayList<Player> teamMembers = new ArrayList<Player>();
    public int turnTeamSize = 0;
    public int voteTally = 0;
    public int voteCount = 0;
    public int prVoteCount = 0;
    public int prVoteTally = 0;
    public int[] score = {0, 0};

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

        players.clear();
        step = 0;
        pmIndex=0;
        turnCounter=0;
        sanitizeTeam();
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
        else{
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text(players.size() + " players in party. Input `ready` to start or add up to 8 players."));
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
                begin(channelId);
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
            turnTeamSize = missionSize[turnCounter];
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Turn " + (turnCounter+1) + " begins.\n" +
                "<@" + pm.getId() + "> is the Product Manager, please choose a team of " + turnTeamSize + ". Input them as @person, each in its own message." ));
        }
        catch(Exception e)
        {
            System.out.println("oof turnPhase");
        }
    }

    public void addTeamMember(String mentionId)
    {
        if(turnTeamSize <= teamMembers.size())
        {
            try{
                ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
                .channel(channelId)
                .text("Team too big, abort add!"));
                return;
            }
            catch(Exception e)
            {
                System.out.println("oof addTeamMember");
            }
        }
        String userId = mentionId.substring(2, mentionId.length()-1);
        System.out.println(userId);
        
        boolean found = false;
        int counter = 0;
        while(!found)
        {
            if(players.get(counter).getId().equals(userId))
            {
                teamMembers.add(players.get(counter));
                found = true;
                System.out.println("Found and added to team.");
            }
            else{
                counter++;
            }
        }

        if(teamMembers.size() >= turnTeamSize)
        {
            System.out.println("before team ready");
            teamReady();
        }

    }

    public void teamReady()
    {
        step++;
        System.out.println("before team ready try");
        try{
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Potential team is ready!"));
            String msg = "The potential team is: ";
            
            int counter = 0;
            while(counter < teamMembers.size())
            {
                msg+= "<@"+teamMembers.get(counter).getId()+"> ";
                counter++;
            }
            msg+= "\nPlease cast your votes: `1` to Approve team and `0` to Veto team.";
            
            String finalMsg = msg;

            ChatPostMessageResponse response2 = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text(finalMsg));
            }
            catch(Exception e)
            {
                System.out.println("oof team ready");
            }
    }

    public void castTeamVote(String vote)
    {

        voteTally += Integer.parseInt(vote);
        voteCount++;
        String finalMsg = voteCount+" vote(s) have been cast. Waiting on " + (players.size()-voteCount) + " more!";
        try{
            ChatPostMessageResponse response2 = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text(finalMsg));
        }
        catch(Exception e)
        {
            System.out.println("oof castTeamVote");
        }
        if(voteCount >= players.size())
        {
            if(voteTally > (players.size()/2))
            {
                try{
                    ChatPostMessageResponse response2 = slack.methods(token).chatPostMessage(req -> req
                    .channel(channelId)
                    .text("The team was approved! Time to code :male-technologist::female-technologist:"));
                }
                catch(Exception e)
                {
                    System.out.println("oof addTeamMember");
                }
                step++;
                prSetup();
            }
            else{
                try{
                    ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
                    .channel(channelId)
                    .text("The team was rejected. Hiring new Product Manager..."));
                }
                catch(Exception e)
                {
                    System.out.println("oof castTeamVote");
                }
                turnCounter--;
                nextTurn();
            }
        }
    }

    public void nextTurn()
    {
        if(isGameOver())
        {
            concludeGame();
        }
        else
        {
            sanitizeTeam();
            step = 2;
            turnCounter++;
            pmIndex++;
            if(pmIndex == players.size())
                pmIndex = 0;
            turnPhase();
        }
    }

    public void sanitizeTeam()
    {
        teamMembers.clear();
        turnTeamSize = 0;
        voteCount = 0;
        voteTally = 0;
        prVoteCount = 0;
        prVoteTally=0;
    }

    public boolean isGameOver()
    {
        if(score[0] >= 3 || score[1]>= 3)
        {
            return true;
        }
        return false;
    }

    public void concludeGame()
    {
        if(score[0] >= 3)
        {
            try{
                ChatPostMessageResponse response1 = slack.methods(token).chatPostMessage(req -> req
                .channel(channelId)
                .text("The Devs pulled through! Great People Make A Great Company!"));
            }
            catch(Exception e)
            {
                System.out.println("oof conclude");
            }
        }
        else{
            try{
                ChatPostMessageResponse response2 = slack.methods(token).chatPostMessage(req -> req
                .channel(channelId)
                .text("The Spies doubled our bug count! Curse these BackYardigans!"));
            }
            catch(Exception e)
            {
                System.out.println("oof conclude");
            }
        }

        step = 5;
        try{
            ChatPostMessageResponse response3 = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("Good game! Hope you had fun! Reply `again` for another round or `end` to pack up the game."));
        }
        catch(Exception e)
        {
            System.out.println("oof conclude");
        }
    }

    public void prSetup()
    {
        int[][] flip = {{0, 1}, {1, 0}};
        Random gen = new Random();
        try{
            ChatPostMessageResponse response2 = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text("The team will now be Dm'd their voting scheme. Please check and then cast your votes `HERE IN THIS CHANNEL`"));
        }
        catch(Exception e)
        {
            System.out.println("oof prSetup");
        }
        int counter = 0;
        while(counter < teamMembers.size())
        {
            int scheme = gen.nextInt(2);
            teamMembers.get(counter).setVoteScheme(scheme);
            String finalMsg = "Vote `"+flip[scheme][0]+"` for clean, juicy, scalable code or Vote `"+flip[scheme][1]+"` for bug injection";
            String chan = teamMembers.get(counter).getId();
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

    public void castPRVote(String vote, String id)
    {
        int[][] flip = {{0, 1}, {1, 0}};
        boolean found = false;
        int counter = 0;
        Player voter = null;
        while(!found)
        {
            if(teamMembers.get(counter).getId().equals(id))
            {
                voter = teamMembers.get(counter);
                found = true;
            }
            else{
                counter++;
            }
        }
        int voteParse = Integer.parseInt(vote);
        int[] scheme = flip[voter.getVoteScheme()];
        System.out.println("PrCount:" + prVoteCount + " PrTally: "+prVoteCount);
        prVoteCount++;

        String finalMsg2 = prVoteCount+" commits have been pushed. Waiting on " + (teamMembers.size()-prVoteCount) + " more!";
        try{
            ChatPostMessageResponse response2 = slack.methods(token).chatPostMessage(req -> req
            .channel(channelId)
            .text(finalMsg2));
        }
        catch(Exception e)
        {
            System.out.println("oof castTeamVote");
        }
        if(scheme[0] == voteParse)
        {
            prVoteTally++;
        }

        if(prVoteCount >= teamMembers.size())
        {
            if(prVoteTally == teamMembers.size())
            {
                try{
                    ChatPostMessageResponse response1 = slack.methods(token).chatPostMessage(req -> req
                    .channel(channelId)
                    .text("The PR was flawless! Great work devs! :male-technologist::female-technologist:"));

                    score[0]++;
                }
                catch(Exception e)
                {
                    System.out.println("oof addPRVote");
                }
            }
            else{
                try{
                    ChatPostMessageResponse response2 = slack.methods(token).chatPostMessage(req -> req
                    .channel(channelId)
                    .text("The PR was riddled with bugs! Master broke! SOMEONE CALL A TRIAGE!"));
                    score[1]++;
                }
                catch(Exception e)
                {
                    System.out.println("oof addPRVote");
                }
            }
            String finalMsg = "There were "+prVoteTally+" good commits and "+(teamMembers.size()-prVoteTally)+" broken ones.";
            try{
                ChatPostMessageResponse response2 = slack.methods(token).chatPostMessage(req -> req
                .channel(channelId)
                .text(finalMsg));
            }
            catch(Exception e)
            {
                System.out.println("oof addPRVote");
            }
            nextTurn();
        }

    }



}