package voss.shared.logic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import voss.shared.logic.support.Shuffler;
import voss.shared.roles.Role;

public class PlayerList implements Iterable<Player>{

	ArrayList<Player> list;
	
	public PlayerList() {
		list = new ArrayList<>();
	}

	public PlayerList(Player ... all){
		this();
		for (Player p: all)
			add(p);
	}
	
	public PlayerList getLivePlayers(){
		PlayerList live = new PlayerList();
		for(Player p: list){
			if(p.isAlive())
				live.add(p);
		}
		return live;
	}

	public PlayerList getDeadPlayers() {
		PlayerList live = new PlayerList();
		for(Player p: list){
			if(p.isDead())
				live.add(p);
		}
		return live;
	}

	public PlayerList add(Player player) {
		if(player != null)
			list.add(player);
		return this;
	}

	public PlayerList add(PlayerList toImport){
		for (Player p: toImport){
			add(p);
		}
		return this;
	}

	public void clear() {
		list.clear();
	}

	public boolean contains(Player p) {
		if (p == null)
			return false;
		return list.contains(p);
	}

	public PlayerList shuffle(Random rand) {
		if(list.isEmpty())
			return this;
		Shuffler.shuffle(list, rand);
		return this;
	}

	public Player remove(int i) {
		return list.remove(i);
	}

	public int size() {
		return list.size();
	}

	public Iterator<Player> iterator() {
		return new PlayerIterator();
	}

	public class PlayerIterator implements Iterator<Player> {

		private int currentIndex = 0;
		private int currentSize = list.size();
		
        @Override
        public boolean hasNext() {
            return currentIndex < currentSize;
        }

	    public Player next() {
	    	return list.get(currentIndex++);
	    }

	    public void remove() {
	        //implement... if supported.
	    }
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public PlayerList remove(Player p) {
		list.remove(p);
		return this;
	}
	public PlayerList remove(PlayerList toRemove){
		for(Player p: toRemove)
			remove(p);

		return this;
	}


	public static PlayerList clone(PlayerList list){
		PlayerList cList = new PlayerList();
		for(Player p: list){
			cList.add(p);
		}
		return cList;
	}
	public static ArrayList<PlayerList> permutations(PlayerList list){
		ArrayList<PlayerList> perms = new ArrayList<PlayerList>();
		if(list.size() == 0)
			return perms;
		if(list.size() == 1){
			perms.add(list);
			return perms;
		}
		Player begin = list.remove(0);
		ArrayList<PlayerList> prev = permutations(list);
		
		for(PlayerList list_: prev)
			for(int i = 0; i <= list_.size(); i++){
				PlayerList perm = clone(list_);
				perm.add(i, begin);
				perms.add(perm);
			}
			
		return perms;
	}

	private void add(int i, Player p) {
		if (p == null)
			return;
		list.add(i, p);
	}

	public Player getRandom(Random rand){
		if (list.size() == 0){
			return null;
		}
		return list.get(rand.nextInt(list.size()));
	}

	public void sendMessage(String s){
		for (Player p: list)
			p.sendMessage(s);
	}

	public Player get(int i) {
		return list.get(i);
	}

	public int indexOf(Player p) {
		return list.indexOf(p);
	}
	
	public String toString(){
		StringBuilder s = new StringBuilder("");
		for(Player p: list){
			s.append(p + ", ");
		}
		if(!list.isEmpty()) {
			s.deleteCharAt(s.length() - 1);
			s.deleteCharAt(s.length() - 1);
		}
		return s.toString();
	}

	public Player removeLast(int i) {
		
		return list.remove(list.size()-1);
	}

	public void sortBySubmissionTime() {
		Collections.sort(list, Player.SubmissionTime);
	}
	public PlayerList sortByName(){
		Collections.sort(list, Player.NameSort);
		return this;
	}
	public PlayerList sortById() {
		Collections.sort(list, Player.IdSort);
		return this;
	}

	public String getStringName(){
		String name = "";
		if (list.size() == 0){
			return name;
		}

		if (list.size() == 1){
			return list.get(0).getName();
		}

		if (list.size() == 2){
			return list.get(0).getName() + " and " + list.get(1).getName();
		}

		for (int i = 0; i < list.size(); i++) {
			Player p = list.get(i);
			if (i + 2 == list.size()) {
				name += p.getName() + ",  and ";
			} else if (i + 1 == list.size() ){
				name += p.getName();
			}else
				name += (p.getName() + ", ");
		}
		return name;
	}

	public boolean equals(Object o){
		if(o == null)
			return false;
		if(o == this)
			return true;
		if(o.getClass() != getClass())
			return false;
		
		PlayerList plist = (PlayerList) o;
		
		PlayerList p1 = this.copy().sortById();
		PlayerList p2 = plist.copy().sortById();
		
		return p1.list.equals(p2.list);
	}

	public void remove(Role r) {
		Player p = null;
		for(Player ps : list){
			if(ps.getRole() == r){
				p = ps;
				break;
			}
		}
		
		list.remove(p);
	}

	public PlayerList copy(){
		PlayerList newList = new PlayerList();
		for (Player p :list)
			newList.add(p);

		return newList;
	}


	public boolean hasName(String s){
		for(Player p: list){
			if (p.getName().toLowerCase().equals(s.toLowerCase()))
				return true;
		}


		return false;
	}


	public String[] getNamesToStringArray(){
		String [] arrayOfNames = new String[list.size()];

		for(int i = 0; i < list.size(); i++)
			arrayOfNames[i] = list.get(i).getName();

		return arrayOfNames;

	}

	

	
}
;