package hello;
import java.util.*;
import java.io.*;

public class Player {
    public String id;
    public boolean spy;

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

}