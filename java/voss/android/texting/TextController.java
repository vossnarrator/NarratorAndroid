package voss.android.texting;

import voss.shared.ai.Controller;
import voss.shared.logic.Player;
import voss.shared.logic.exceptions.UnsupportedMethodException;
import voss.shared.logic.support.Constants;
import voss.shared.roles.Arsonist;
import voss.shared.roles.Mayor;



public class TextController extends Controller {

    private TextInput texter;
    public TextController(TextInput texter){
        this.texter = texter;
    }

    public void log(String string) {

    }


    public void endNight(Player slave) {
        texter.text(slave, TextHandler.END_NIGHT);
    }

    public void cancelEndNight(Player slave) {
        endNight(slave);
    }

    public void setNightTarget(Player a, Player b, String action) {
    	b = Translate(a.getNarrator(), b);
        texter.text(a, action + " " + b.getName());
    }

    public void setNightTarget(Player a, Player b, String action, String teamName) {
    	b = Translate(a.getNarrator(), b);
        texter.text(a, action + " " +  b.getName() + " " + teamName);
    }
    
    public void removeNightTarget(Player a, String action) {
		texter.text(a, action + " " + Constants.UNTARGET);
	}

    public void vote(Player slave, Player target) {
    	target = Translate(slave.getNarrator(), target);
    	if(target == slave.getSkipper())
    		skipVote(slave);
    	else
    		texter.text(slave, TextHandler.VOTE + " " + target.getName());
        
    }

    public void selectHost(Player host) {

    }
    
    public void skipVote(Player a){
    	texter.text(a, TextHandler.SKIP_VOTE);
    }


	public void say(Player slave, String message) {
		texter.text(slave, TextHandler.SAY + " " + message);
	}


	public void doDayAction(Player p) {
		if(p.is(Mayor.ROLE_NAME)){
			texter.text(p, Mayor.REVEAL);
			return;
		}
		else if(p.is(Arsonist.ROLE_NAME)){
			texter.text(p, Arsonist.BURN);
			return;
		}
		throw new UnsupportedMethodException();
	}

	public void unvote(Player slave) {
		texter.text(slave, TextHandler.UNVOTE);
		
	}

	
}
