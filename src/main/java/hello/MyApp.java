package hello;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import hello.ConversationTest;
import hello.GameDriver;

import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.response.chat.ChatGetPermalinkResponse;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.methods.response.reactions.ReactionsAddResponse;
import com.slack.api.model.event.MessageEvent;

import java.util.regex.Pattern;

public class MyApp {


  public static void main(String[] args) throws Exception {
    // App expects env variables (SLACK_BOT_TOKEN, SLACK_SIGNING_SECRET)
    try{
    App app = new App();
    GameDriver game = new GameDriver();
    
    //check for all msgs
    Pattern sdk = Pattern.compile(".*", Pattern.CASE_INSENSITIVE);
    app.message(sdk, (payload, ctx) -> {
      try{
        //parse JSON for msg text
        MessageEvent event = payload.getEvent();
        String text = event.getText();
        

        //initialize game
        if(game.getStep() == 0 && text.contains("start game"))
        {
          String chan = event.getChannel();
          System.out.println("Step 0");
          game.begin(chan);
        }

        //opt in phase
        else if(game.getStep() == 1  && text.contains("opt-in"))
        {
          System.out.println("Player added");
          String playerId = event.getUser();
          game.addPlayer(playerId);
        }
        //begin 
        else if(game.getStep() == 1  && text.contains("ready"))
        {
          game.startTurnPhase();
        }
        else if(game.getStep() == 2 && text.contains("<"))
        {
          game.addTeamMember(text);
        }
        else if(game.getStep() == 3 && (text.contains("1") || text.contains("0")) && !text.contains("<"))
        {
          System.out.println("entered voting");
          game.castTeamVote(text);
        }
        else if(game.getStep() == 4 && (text.contains("1") || text.contains("0")) && !text.contains("<"))
        {
          String playerId = event.getUser();
          game.castPRVote(text, playerId);
        }
        else if(game.getStep() == 5 && text.contains("end"))
        {
          game.resetGame();
        }
        else if(game.getStep() == 5 && text.contains("again"))
        {
          String chan = event.getChannel();
          game.resetGame();
          game.begin(chan);
        }
        else if(text.contains("restart"))
        {
          String chan = event.getChannel();
          game.resetGame();
        }
        System.out.println("all steps "+ game.getStep() + " " + text);
      }
      catch(Exception e)
      {
        System.out.println(e);
      }
      return ctx.ack();
    });
    
    /*app.command("/hello", (req, ctx) -> {
      return ctx.ack(":wave: Hello!");
    });*/
    /*Pattern sdk = Pattern.compile("sup|start", Pattern.CASE_INSENSITIVE);
    app.message(sdk, (payload, ctx) -> {
        ConversationTest temp = new ConversationTest();
        try{
        
        MessageEvent event = payload.getEvent();
        String text = event.getUser();
        System.out.println(text);
        //temp.breakerTest();
        }
        catch(Exception e)
        {
            System.out.println("oof");
        }
        return ctx.ack();
      });*/

      /*app.message("start", (payload, ctx) -> {
        System.out.println("Starting");
        ConversationTest temp = new ConversationTest();
        try{
          temp.breakerTest();
          System.out.println("done");
        }
        catch(Exception e)
          {
              System.out.println("oof");
          }
          return ctx.ack();
    });*/


    SlackAppServer server = new SlackAppServer(app);
    server.start(); // http://localhost:3000/slack/events
    }
    catch(Exception e)
    {
        System.out.println("oof");
    }

    
  }
}