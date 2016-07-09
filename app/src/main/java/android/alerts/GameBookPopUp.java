package android.alerts;


import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.parse.GameListing;
import android.parse.Server;
import android.screens.ActivityHome;
import android.screens.ListingAdapter;
import android.setup.SetupManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import shared.logic.Member;
import shared.logic.Narrator;
import shared.logic.support.CommandHandler;
import shared.logic.support.RoleTemplate;
import shared.roles.RandomRole;
import shared.roles.Role;
import voss.narrator.R;

public class GameBookPopUp extends DialogFragment implements Server.GameFoundListener, AdapterView.OnItemClickListener, View.OnClickListener{

    public static final int RESUME = 0;
    public static final int JOIN = 1;

    private View mainView;
    private ListView gameLV;
    private EditText searchBar;
    private Button goButton;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        mainView = inflater.inflate(R.layout.create_player_list, container);


        searchBar = (EditText) mainView.findViewById(R.id.addPlayerContent);
        goButton = (Button) mainView.findViewById(R.id.addPlayerConfirm);
        gameLV = (ListView) mainView.findViewById(R.id.listView1);

        if (mode == JOIN) {
            Server.GetAllGames(15, this);
            goButton.setText("Search");
            setTitle("Open Games:");
        }else{ //resume
            Server.GetMyGames(this);
            goButton.setText("Continue");
            setTitle("Current Games");
        }
        goButton.setOnClickListener(this);
        return mainView;
    }

    private int mode;
    public void setMode(int mode){
        this.mode = mode;
    }

    private ArrayList<GameListing> games;
    private HashMap<String, GameListing> hostToGame;

    public void onGamesFound(ArrayList<GameListing> games){
        this.games = games;
        hostToGame = new HashMap<>();
        ArrayList<String> toAdd = new ArrayList<>();

        ArrayList<String> colors = new ArrayList<>();
        final String started = "#00FF00";//ActivityCreateGame.ParseColor(a, R.color.green);
        final String waiting = "#FFFF00";//ActivityCreateGame.ParseColor(a, R.color.yellow);

        for(GameListing gl : games) {
            hostToGame.put(gl.getHostName(), gl);
            toAdd.add(gl.getHeader());
            if(gl.inProgress())
                colors.add(started);
            else
                colors.add(waiting);
        }
        ListingAdapter adapter = new ListingAdapter(toAdd, a);
        adapter.setTextSize(15);
        adapter.setColors(colors);
        gameLV.setAdapter(adapter);
    }

    public void noGamesFound(){
        a.toast("No games found");
    }

    public void onInvalidToken(){
        a.toast("Something strange happened. Log out and log back in again.");
    }

    public void onError(String error){
        a.toast(error);
    }

    public void onClick(View v){
        if(hostToGame == null)
            return;

        String potentialHostName = getSearchContents();
        if (potentialHostName.length() == 0)
            return;



        if (!hostToGame.containsKey(potentialHostName)){
            a.toast("No games are being run by that host");
        }else {
            joinGame(hostToGame.get(potentialHostName), a, mode);
            dismiss();
        }
    }

    public void onItemClick(AdapterView<?> av, View v, int i, long l){
        String prevText = getSearchContents();

        GameListing gameSelected = games.get(i);
        String hostName = gameSelected.getHostName();

        if(hostName.equals(prevText)) {
            joinGame(gameSelected, a, mode);
            dismiss();
        }
        setSearchBar(hostName);
    }

    private String getSearchContents(){
        return searchBar.getText().toString();
    }

    private void setSearchBar(String s){
        searchBar.setText(s);
    }

    public static synchronized void joinGame(GameListing gl, ActivityHome a, int mode){
        Server.Channel(gl);

        a.ns.refresh();
        Narrator n = a.ns.local;
        for (String name: gl.getPlayerNames()){
            n.addPlayer(name);
        }
        for (String roleCompact: gl.getRoleNames()){
            RoleTemplate rt = RoleTemplate.FromIp(roleCompact);
            rt = SetupManager.TranslateRole(rt);
            if (Role.isRole(rt.getName()))
                n.addRole((Member) rt);
            else
                n.addRole((RandomRole) rt);
        }
        //n.setRules(gl.getRules());
        if (gl.inProgress()) {
            n.setSeed(gl.getSeed());
            n.startGame();
        }
        CommandHandler ch = new CommandHandler(n);
        String[] parts;
        String sender;
        for (String s : gl.getCommands()) {
            parts =  s.split(",");
            sender = parts[0];
            s = s.substring(sender.length() + 1);
            try {
                ch.parseCommand(s);
            }catch(Throwable e){}
        }

        if (mode == JOIN){
            Server.AddPlayer(gl);
            n.addPlayer(Server.GetCurrentUserName());
        }

        a.start(gl);//NewGame(gl.getID(), Server.GetCurrentUserName().equals(gl.getHostName()));

    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        gameLV.setOnItemClickListener(this);
    }

    public void setTitle(String s){
        getDialog().setTitle(s);
    }

    private ActivityHome a;
    public void onAttach(Activity a){
        super.onAttach(a);
        this.a = (ActivityHome) a;
    }
}
