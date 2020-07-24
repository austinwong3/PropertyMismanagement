package hello;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import hello.ConversationTest;

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


    app.command("/hello", (req, ctx) -> {
      return ctx.ack(":wave: Hello!");
    });


    Pattern sdk = Pattern.compile("sup|start", Pattern.CASE_INSENSITIVE);
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
      });

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