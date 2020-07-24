package hello;
import java.util.*;
import java.io.*;

public class Player {
    public String id;
    public boolean spy;
    public int voteScheme;

    Player(String code)
    {
        id = code;
    }

    public void setId(String code)
    {
        id = code;
    } 
    public String getId()
    {
        return id;
    }
    public void setRole(boolean role)
    {
        spy = role;
    }
    public boolean isSpy()
    {
        return spy;
    }
    public void setVoteScheme(int num)
    {
        voteScheme = num;
    }
    public int getVoteScheme()
    {
        return voteScheme;
    }

}