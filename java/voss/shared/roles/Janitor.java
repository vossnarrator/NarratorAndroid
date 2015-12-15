package voss.shared.roles;

import java.util.ArrayList;

import voss.shared.logic.Event;
import voss.shared.logic.Narrator;
import voss.shared.logic.Player;


public class Janitor extends Role{
	
	public static final String ROLE_NAME = "Janitor";
	public String getRoleName(){
		return ROLE_NAME;
	}
	
	public Janitor(Player p){
		super(p);
	}
	
	public static final String NIGHT_PROMPT = "You have the ability to hide the role of a person from being annouced to the town.";
	public String getRoleInfo() {
		return NIGHT_PROMPT;
	}
	
	public static final int CLEAN = MAIN_ABILITY;

	public void isAcceptableTarget(Player owner, Player target, int ability) {
		deadCheck(target);
		allowedAbilities(ability, CLEAN);
		selfCheck(owner, target);
	}



	public static final String DEATH_FEEDBACK = "You were killed by the mafia!";
	public boolean doNightAction(Player owner, Narrator n) {
		Player target = owner.getHouseTarget(MAIN_ABILITY);
		if(target == null)
			return false;
		owner.visit(target);
		if(!target.isAlive())
			target.setCleaned(true);
		Event e = new Event();
		e.add(owner, " cleaned ", target, ".");
		e.setPrivate();
		n.addEvent(e);
		return true;
	}

	private static final String COMMAND = "Clean";


	
	
	


	public ArrayList<String> getAbilities() {
		ArrayList<String> list = new ArrayList<String>();
		list.add(COMMAND);
		return list;
	}
	
	
	
	
	
}