package voss.shared.logic.support;

import java.util.HashMap;

import voss.shared.logic.Player;


public class StringChoice {

    private Object def;
    private HashMap<String, Object> idToString = new HashMap<>();

    public StringChoice(Object def){
        this.def = def;
    }

    public Object getString(String level){
        Object ret = idToString.get(level);
        if(ret == null)
            ret = def;
        return ret;
    }

    public StringChoice add(String level, String word){
        idToString.put(level, word);
        return this;
    }
    
    public StringChoice add(Player p, String word){
    	return add(p.getName(), word);
    }

}
