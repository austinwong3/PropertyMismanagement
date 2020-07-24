package hello;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.bolt.App;
import com.slack.api.bolt.jetty.SlackAppServer;
import com.slack.api.methods.response.api.ApiTestResponse;





public class ConversationTest {

    public boolean phase = false;

  public void setPhase()
  {
    phase = true;
    System.out.println(true);
  }

    public void Convo(String ident) throws Exception {

            Slack slack = Slack.getInstance();
            String token = System.getenv("SLACK_BOT_TOKEN");

            ApiTestResponse res = slack.methods().apiTest(r -> r.foo("bar"));
            System.out.println(res);

            // Initialize an API Methods client with the given token
            MethodsClient methods = slack.methods(token);

            // Build a request object
            ChatPostMessageResponse response = slack.methods(token).chatPostMessage(req -> req
            .channel("#random")
            .text("<@"+ident+"> sucks!"));

            System.out.println(response);
            System.out.println("done");

    }

    public void breakerTest()
    {
        try{
        App app = new App();
        app.message("vibin", (payload, ctx) -> {
            System.out.println("yoooooo");
            return ctx.ack();
          });
        System.out.println("After vibes");

        }
        catch(Exception e)
        {
            System.out.println("yikeroni");
        }
    }


    }

