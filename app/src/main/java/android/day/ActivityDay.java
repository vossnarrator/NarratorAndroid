package android.day;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.google.firebase.auth.FirebaseAuth;

import android.GUIController;
import android.GameState;
import android.JUtils;
import android.NActivity;
import android.alerts.ExitGameAlert;
import android.alerts.ExitGameAlert.ExitGameListener;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.day.PlayerDrawerAdapter.OnPlayerClickListener;
import android.graphics.Color;
import android.os.Bundle;
import android.screens.ListingAdapter;
import android.screens.MembersAdapter;
import android.screens.SimpleGestureFilter;
import android.screens.SimpleGestureFilter.SimpleGestureListener;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.DrawerLayout.DrawerListener;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.texting.PhoneNumber;
import android.texting.StateObject;
import android.texting.TextHandler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import shared.event.OGIMessage;
import shared.logic.Member;
import shared.logic.Player;
import shared.logic.PlayerList;
import shared.logic.exceptions.IllegalActionException;
import shared.logic.exceptions.PlayerTargetingException;
import shared.roles.Assassin;
import shared.roles.Framer;
import voss.narrator.R;


public class ActivityDay extends NActivity
implements 
	ExitGameListener, 
	OnClickListener, 
	OnInitListener, 
	OnItemSelectedListener, 
	OnPlayerClickListener, 
	DrawerListener, 
	SimpleGestureListener {

	public DayManager manager;

	private IntentFilter iF;
	private TextToSpeech speaker;
	public ListView rolesLV, membersLV, actionLV, alliesLV;
	public TextView membersTV, rolesTV, roleTV, roleInfoTV, alliesTV, commandTV, playerLabelTV;
	public Spinner framerSpinner;
	private ListView chatLV;
	public ChatAdapter chatAdapter;
	public EditText chatET;
	public Button button, chatButton, messagesButton, actionButton, infoButton;

	private DrawerLayout dayWindow;
	public RecyclerView playerMenu;
	
	private SimpleGestureFilter detector;


	protected void onCreate(Bundle b){
		super.onCreate(b);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_day);

		setup(b);

		iF = new IntentFilter();
		iF.addAction("SMS_RECEIVED_ACTION");
		//iF.addAction(ParseConstants.PARSE_FILTER);
	}
	
	protected void onResume(){
		super.onResume();
		setup(null);
		registerReceiver(intentReceiver, iF);
	}

	//protected void onSaveInstanceState(Bundle b){
		//super.onSaveInstanceState(b);
		//if (b != null)
			//b.putParcelable(Narrator.KEY, Board.GetParcel(manager.getNarrator()));
	//}
	public void onBackPressed(){
		if (!ns.isInProgress()){
			stopTexting();
			speaker.stop();
			speaker.shutdown();
			ns.gameState = new GameState(ns);
			finish();
		}
		if (drawerOut){
			closeDrawer();
		}else {
			if(onePersonActive()){
				if (manager.getCurrentPlayer() == null)
					try {
						onPlayerClick(playersInDrawer.getString(0));
					} catch (JSONException e) {
						e.printStackTrace();
					}
				else
					onPlayerClick(null);
			}else
				onPlayerClick(null);
			onDrawerClosed(null);

			if(!ns.server.IsLoggedIn())
				onClick(infoButton);

		}
	}

	public List<Member> setMembers(){
		List<Member> list = new ArrayList<>();

		return list;
	}

	public void updateChat(){
		manager.dScreenController.updateChatPanel();
	}

	public void setup(Bundle b){
		if (playerMenu != null)
			return;
		
		playerMenu = (RecyclerView) findViewById(R.id.day_playerNavigationPane);


		dayWindow = (DrawerLayout) findViewById(R.id.day_main);
		dayWindow.setDrawerListener(this);
		dayWindow.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

		chatLV         = (ListView) findViewById(R.id.day_chatHolder);
		chatET         = (EditText) findViewById(R.id.day_chatET);
		chatET.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					sendMessage();
					return true;
				}
				else if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
					sendMessage();
					return true;
				}
				return false;
			}
		});


		commandTV = (TextView) findViewById(R.id.day_commandsLabel);
		actionLV       = (ListView) findViewById(R.id.day_actionList);

		roleTV         = (TextView) findViewById(R.id.day_roleLabel);
		roleInfoTV     = (TextView) findViewById(R.id.day_role_info);
		roleInfoTV.setText("Players of Society");
		int color = ParseColor(this, R.color.trimmings);
		roleInfoTV.setTextColor(color);

		//chatTV         = (TextView) findViewById(R.id.day_chatTV);
		playerLabelTV  = (TextView) findViewById(R.id.day_currentPlayerTV);

		membersTV      = (TextView) findViewById(R.id.day_membersLabel);
		membersLV      = (ListView) findViewById(R.id.day_membersLV);

		rolesTV        = (TextView) findViewById(R.id.day_rolesList_label);
		rolesLV        = (ListView) findViewById(R.id.day_rolesList);

		alliesTV       = (TextView) findViewById(R.id.day_alliesLabel);
		alliesLV       = (ListView) findViewById(R.id.day_alliesList);

		button         = findButton(R.id.day_button); //action button for mayor, arson, and end night
		messagesButton = findButton(R.id.day_messagesButton);
		infoButton     = findButton(R.id.day_infoButton);
		actionButton   = findButton(R.id.day_actionButton);
		chatButton     = findButton(R.id.day_chatButton);

		framerSpinner  = (Spinner) findViewById(R.id.day_frameSpinner);
		framerSpinner.setOnItemSelectedListener(this);
		if(wideMode())
			framerSpinner.setGravity(Gravity.CENTER);
		else
			framerSpinner.setGravity(Gravity.END);

		detector = new SimpleGestureFilter(this, this);

		connectNarrator(new NarratorConnectListener() {
			public void onConnect() {
				connectManager();
			}

		});
		
		
		button.setOnClickListener(this);
		chatButton.setOnClickListener(this);

		addOnClickListener(R.id.day_actionButton);
		addOnClickListener(R.id.day_messagesButton);
		addOnClickListener(R.id.day_infoButton);
		addOnClickListener(R.id.day_playerDrawerButton);
		addOnClickListener(R.id.day_chatET);

		setHeaderFonts(R.id.day_title, R.id.day_currentPlayerTV, R.id.day_actionButton, R.id.day_roleLabel, R.id.day_messagesButton, R.id.day_infoButton, R.id.day_alliesLabel, R.id.day_rolesList_label, R.id.day_membersLabel, R.id.day_button);
		setLowerFonts(R.id.day_chatButton, R.id.day_commandsLabel);


		speaker = new TextToSpeech(this, this);
		speaker.setLanguage(Locale.UK);
		speaker.setSpeechRate(0.9f);

	}
	private void setLowerFonts(int ... ids){
		for (int id: ids){
			SetFont(id, this, false);
		}
	}
	private void setHeaderFonts(int ... ids){
		for (int id: ids){
			SetFont(id, this, true);
		}
	}
	private void connectManager(){
		if(manager != null)
			return;

		manager = new DayManager(ns);
		manager.initiate(this);

		onClick(infoButton);

		if(!ns.isInProgress()) {
			endGame();
			return;
		}
		String topInDrawer = JUtils.getString(playersInDrawer, 0);
		if(onePersonActive()) {
			toast("Press back to switch between general information and your information.");
			if(ns.server.IsLoggedIn())
				if(ns.gameState.isAlive){
					GUIController.selectScreen(this, topInDrawer);
			}else{
				if(ns.local.getPlayerByID(topInDrawer).isAlive())
					GUIController.selectScreen(this, topInDrawer);
			}
		}
	}
	boolean onePersonActive(){
		return playersInDrawer.length() == 1;
	}
	private Button findButton(int id){
		return (Button) findViewById(id);
	}
	private void addOnClickListener(int id){
		findViewById(id).setOnClickListener(this);

	}
	private JSONArray playersInDrawer;
	protected void setupPlayerDrawer(JSONArray livePlayers){
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
		playerMenu.setLayoutManager(layoutManager);
		playerMenu.setAdapter(new PlayerDrawerAdapter(livePlayers, this));
		playersInDrawer = livePlayers;
	}
	public ArrayList<String> frameOptions;
	public void setupFramerSpinner(){
		frameOptions = new ArrayList<>();
		ArrayList<String> teamColors = new ArrayList<>();

		JSONArray activeTeams = ns.getActiveTeams();
		if(activeTeams == null)
				return;
		JSONObject teamObject;
		for (int i = 0; i < activeTeams.length(); i++){
			teamObject = JUtils.getJSONObject(activeTeams, i);
			teamColors.add(JUtils.getString(teamObject, StateObject.color));
			frameOptions.add(JUtils.getString(teamObject, StateObject.teamName));
		}

		ListingAdapter adapter = new ListingAdapter(frameOptions, this);
		adapter.setColors(teamColors);
		adapter.setLayoutID(R.layout.day_player_player_dropdown_item);
		if(framerSpinner == null)
			framerSpinner  = (Spinner) findViewById(R.id.day_frameSpinner);
		framerSpinner.setAdapter(adapter);
	}

	public void log(String i){
		Log.d("ActivityDay", i);
	}

	
	protected void setButtonText(String s){
		if(button == null || s == null)
			throw new NullPointerException("This is null??");
		button.setText(s);
	}
	protected String getSelectedAbility(){
		return commandTV.getText().toString();
	}
	protected void setDayLabel(boolean day, int dayNumber){
		String s;
		if(day)
			s = "Day";
		else
			s = "Night";
		s += " " + dayNumber;
		((TextView) findViewById(R.id.day_title)).setText(Html.fromHtml("<u>" + s + "</u>"));
	}


	public void onInit(int status) {
		
	}
	protected void say(String s) {
		speaker.speak(s, TextToSpeech.QUEUE_ADD, null);
	}


	protected void setCommand(String command){
		commandTV.setText(command);
	}

	protected void updateMembers() {
		JSONArray allPlayers = JUtils.getJSONArray(ns.getPlayers(manager.getCurrentPlayer()), "info");
		membersLV.setAdapter(new MembersAdapter(allPlayers, this));
	}
	protected void uncheck(String name){
		actionLV.setItemChecked(actionList.indexOf(name), false);

	}
	protected void uncheck(ArrayList<String> names){
		for(String name: names)
			uncheck(name);

	}
	protected void check(PlayerList selected){
		if (selected != null){
			for(Player p: selected){
				int index = actionList.indexOf(p);
				if(index == -1){
					selected.toString();
				}
				
				actionLV.setItemChecked(index, true);
			}
		}

	}
	protected void hideDayButton(){
		hideView(button);
	}

	
	
	
	public BroadcastReceiver intentReceiver = new BroadcastReceiver(){
		public void onReceive(Context context, Intent intent){
			String message = intent.getExtras().getString("message");
			if(message == null) {
				manager.parseCommand(intent);
				return;
			}
			PhoneNumber number = new PhoneNumber(intent.getExtras().getString("number"));
			Player owner = manager.phoneBook.getByNumber(number);
			
			if(owner == null){
				Toast.makeText(ActivityDay.this, "received text message", Toast.LENGTH_LONG).show();
				return;
			}
			
			try{
				synchronized(manager.ns.local){
					manager.tHandler.text(owner, message, false);
				}
			}catch(Exception|Error e) {
				e.printStackTrace();
				new OGIMessage(owner, e.getMessage());
			}
		}
	};
	
	
	
	
	


	public TargetablesAdapter targetablesAdapter;
	public ArrayList<String> actionList;
	protected void setActionList(JSONArray playerList, boolean day){
		ArrayList<String> newActionList = new ArrayList<>();
		HashMap<String, ArrayList<Integer>> checkedPositions = new HashMap<>();
		
		JSONObject jPlayer;
		int i;
		JSONArray jArray;
		ArrayList<Integer> selected;
		for(i = 0; i < playerList.length(); i++){
			jPlayer = JUtils.getJSONObject(playerList, i);
			String name = JUtils.getString(jPlayer, StateObject.playerName);
			newActionList.add(name);
			jArray = JUtils.getJSONArray(jPlayer, StateObject.playerSelectedColumn);
			if(!checkedPositions.containsKey(name))
				checkedPositions.put(name, new ArrayList<Integer>());
			selected = checkedPositions.get(name);
			for(int j = 0; j < jArray.length(); j++){
				selected.add(JUtils.getInt(jArray, j));
				
			}
		}
		if(day && manager.getCommand().equals("Vote")){
			newActionList.add("Skip Day");
			selected = new ArrayList<>();
			if(manager.getCurrentPlayer() != null)
				if(ns.isVoting(manager.getCurrentPlayer(), "Skip Day")){
					
					selected.add(0);
			}
			checkedPositions.put("Skip Day", selected);
		}
		
		
		synchronized(manager){
			if(targetablesAdapter == null || actionList == null || !actionList.equals(newActionList)){
				targetablesAdapter = new TargetablesAdapter(manager, newActionList, checkedPositions);
				actionList = newActionList;
				actionLV.setAdapter(targetablesAdapter);
			}else{
				targetablesAdapter.setCheckBoxes(checkedPositions);
			}
		}
		


		actionLV.setOnItemClickListener(targetablesAdapter);
		
	}



	public View panel = null;
	public void onClick(View v){
		switch(v.getId()){
			case R.id.day_chatET:
				pushChatDown();
				break;

			case R.id.day_button:
				log("Big button clicked");
				try{
					manager.buttonClick();
				}catch(PlayerTargetingException|IllegalActionException e){
					e.printStackTrace();
					toast(e.getMessage());
				}
				break;

			case R.id.day_playerDrawerButton:
				log("Player drawer activated");
				dayWindow.openDrawer(playerMenu);
				break;

			case R.id.day_messagesButton:
				log("Message button clicked.");

				if(wideMode())
					return;

				panel = v;
				setSelected(R.id.day_messagesButton);
				hideActionPanel();
				hideInfoPanel();
				showMessagesPanel();
				break;

			case R.id.day_infoButton:
				log("Info button clicked.");

				if(wideMode())
					return;

				panel = v;
				setSelected(R.id.day_infoButton);
				hideActionPanel();
				hideMessagePanel();
				showInfoPanel();
				break;

			case R.id.day_chatButton:
				log("Send Message button clicked.");
				sendMessage();
				break;

			case R.id.day_actionButton:
				log("Actions button clicked.");


				if(wideMode())
					return;

				panel = v;
				setSelected(R.id.day_actionButton);
				hideMessagePanel();
				hideInfoPanel();
				showActionPanel();
		}
	}
	public boolean wideMode(){
		return infoButton.getVisibility() == View.GONE;
	}
	private void setSelected(int id){
		Button b = (Button) findViewById(id);
		b.setTextColor(ParseColor(this, R.color.redBlood));
		int blackColor = ParseColor(this, R.color.black);
		if(id != R.id.day_actionButton)
			actionButton.setTextColor(blackColor);
		if(id != R.id.day_messagesButton)
			messagesButton.setTextColor(blackColor);
		if(id != R.id.day_infoButton)
			infoButton.setTextColor(blackColor);
	}

	public void sendMessage(){
		String message = chatET.getText().toString();
		if (message.length() == 0)
			return;

		if(manager.getCurrentPlayer() == null && !onePersonActive()) {
			if (message.startsWith(TextHandler.MODKILL + " ")) {
				String name = message.substring(TextHandler.MODKILL.length() + 1);
				Player baddy = manager.phoneBook.getByName(name);
				if (baddy != null)
					baddy.modkill();
			}
		} else if(manager.getCurrentPlayer() == null) {
			onBackPressed();
			manager.talk(message);
		}else if(!manager.ns.isDead(manager.getCurrentPlayer())) {
			manager.talk(message);
		}
		chatET.setText("");
	}

	public void updateChatPanel(){
		setChatPanelText();

		ListView chatHolder = (ListView) findViewById(R.id.day_chatHolder);
		/*View cView = chatHolder.getChildAt(chatHolder.getChildCount() - 1);
		int diff = (cView.getBottom()-(chatHolder.getHeight()+chatHolder.getScrollY()));*/
		boolean isAtBottom = chatHolder.getLastVisiblePosition() == chatHolder.getAdapter().getCount() -1 &&
				chatHolder.getChildAt(chatHolder.getChildCount() - 1).getBottom() <= chatHolder.getHeight();
		if(isAtBottom)
			pushChatDown();
	}

	private void setChatPanelText(){
		if(chatAdapter == null){
			chatAdapter = new ChatAdapter(manager.ns.getChat(), this);
			chatLV.setAdapter(chatAdapter);
		}
		chatAdapter.notifyDataSetChanged();
		/*chatLV.getAdapter().notifyDataSetChanged();
		if(progress)
			chatTV.setText(Html.fromHtml(text));*/
	}

	public void hideMessagePanel(){
		chatET.setText("");
		hideView(chatLV);
		hideView(chatButton);
		hideView(chatET);
		hideKeyboard();
	}
	
	private void hideKeyboard(){
		InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		if(getCurrentFocus() != null)
			imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
	}

	public void showInfoPanel(){
		showView(rolesTV);
		showView(rolesLV);

		//showView(rightTV);
		if (manager.dScreenController != null && manager.dScreenController.playerSelected()){
			if (ns.hasDayAction(manager.getCurrentPlayer()) && ns.isDay())
				showView(button);
			else
				hideView(button);

			JSONObject roleInfo = ns.getRoleInfo(manager.getCurrentPlayer());
			boolean shouldShowTeam = roleInfo.has(StateObject.roleTeam);
			if(shouldShowTeam){
				showView(alliesTV);
				showView(alliesLV);
				hideView(rolesTV);
				hideView(rolesLV);
			}else{
				hideView(alliesTV);
				hideView(alliesLV);
				showView(rolesTV);
				showView(rolesLV);
			}

			showView(roleTV);
			showView(roleInfoTV);

			hideView(membersTV);
			hideView(membersLV);
		}else{
			showView(membersTV);
			showView(membersLV);

			hideView(roleTV);
			hideView(roleInfoTV);
			hideView(button);

			hideView(alliesTV);
			hideView(alliesLV);
		}
	}

	public void onPlayerClick(String name){
		manager.setCurrentPlayer(name);
	}

	public void onExitAttempt() {
		DialogFragment newFragment = new ExitGameAlert();
		newFragment.show(getFragmentManager(), "missiles");
	}

	public void onExitGame(){
		stopTexting();
		speaker.stop();
		speaker.shutdown();
		unbindNarrator();
		finish();
	}

	public int getMyColor(int id){
		return getResources().getColor(id);
	}

	public void setActionButton(){
		if(manager.ns.isDay()){
			actionButton.setText("Voting");
		}else{
			actionButton.setText("Actions");
		}
	}

	public void hideActionPanel(){
		((RelativeLayout.LayoutParams)actionLV.getLayoutParams()).addRule(RelativeLayout.BELOW, commandTV.getId());
		hideView(actionLV);
		hideView(commandTV);
		hideView(button);
		hideView(framerSpinner);
	}

	public void showActionPanel() {
		showView(actionLV);
		showView(commandTV);

		showFrameSpinner();
		showButton();
	}

	public void showFrameSpinner(){
		if (!manager.dScreenController.playerSelected() || ns.isDay()) {
			hideView(framerSpinner);
			return;
		}
		String currentPlayer = manager.getCurrentPlayer();
		hideView(framerSpinner);
		if(ns.isDead(currentPlayer))
			return;
		JSONObject roleInfo = manager.ns.getRoleInfo(currentPlayer);
		boolean isFramer = JUtils.getString(roleInfo, StateObject.roleBaseName).equals(Framer.class.getSimpleName());

		if (isFramer && isFrameActionSelected()) {
			showView(framerSpinner);
			if (!wideMode())
				((RelativeLayout.LayoutParams) actionLV.getLayoutParams()).addRule(RelativeLayout.BELOW, framerSpinner.getId());
		}

	}

	public void showButton() {
		String currentPlayer = manager.getCurrentPlayer();
		if(currentPlayer == null || manager.ns.isDead(currentPlayer)){
			hideView(button);
		}else if (wideMode()) {
			if(ns.isNight())
				showView(button);
			else if(manager.ns.hasDayAction(currentPlayer))
				showView(button);
			else
				hideView(button);
			
		}else{
			if(messagesButton == panel){
				hideView(button);
			}else if(panel == actionButton){
				if(manager.getCurrentPlayer() == null)
					hideView(button);
				else if(ns.isDay()){
					if(commandTV.getText().toString().equalsIgnoreCase(Assassin.ASSASSINATE))
						showView(button);
					else
						hideView(button);
				}else
					showView(button);
			}else{ //panel == infoButton
				if(ns.isNight() || !manager.ns.hasDayAction(currentPlayer))
					hideView(button);
				else
					showView(button);
			}
		}
	}

	private boolean isFrameActionSelected(){
		return commandTV.getText().toString().equals(Framer.FRAME);
	}

	public void hideInfoPanel(){
		hideView(rolesTV);
		hideView(rolesLV);

		hideView(membersTV);
		hideView(membersLV);

		hideView(alliesLV);
		hideView(alliesTV);

		hideView(roleTV);
		hideView(roleInfoTV);
		hideView(button);
	}

	public void showMessagesPanel() {
		showView(chatLV);
		if(manager.dScreenController.playerSelected()) {
			showView(chatET);
			showView(chatButton);
		}
		pushChatDown();
	}

	public void pushChatDown() {
		/*chatLV.post(new Runnable() {
			public void run() {
				chatLV.fullScroll(View.FOCUS_DOWN);
			}
		});*/
		
	}

	public void setPlayerLabel(String name) {
		playerLabelTV.setText(name);
	}

	protected void hideView(View v){
		v.setVisibility(View.GONE);
	}
	protected void hideView(int id){
		findViewById(id).setVisibility(View.GONE);
	}

	protected void showView(View v){
		v.setVisibility(View.VISIBLE);
	}

	protected void setListView(ListView v, ArrayList<String> texts, ArrayList<String> colors){
		setListView(v, texts, colors, texts.size());
	}

	protected void setListView(ListView v, ArrayList<String> texts, ArrayList<String> colors, int limit){
		ListingAdapter ad = new ListingAdapter(texts, this).setColors(colors);
		ad.setLimit(limit);
		v.setAdapter(ad);
	}

	public void endGame(){

		hideActionPanel();
		hideInfoPanel();
		hideMessagePanel();
		hideView(playerLabelTV);
		hideView(R.id.day_playerDrawerButton);
		hideView(R.id.day_title);
		hideView(R.id.day_horizontalShimmy);
		hideView(messagesButton);
		hideView(actionButton);
		hideView(infoButton);
		hideView(commandTV);

		hideView(roleInfoTV);
		hideView(roleTV);

		showView(chatLV);

		((RelativeLayout.LayoutParams)chatLV.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_TOP);

		if(wideMode()){
			findViewById(R.id.day_chatHolder).setVisibility(View.VISIBLE);
			findViewById(R.id.day_chatTV).setVisibility(View.VISIBLE);
			int in = android.os.Build.VERSION.SDK_INT;
			if(in >= 17){
				((RelativeLayout.LayoutParams)chatLV.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_END);
				((RelativeLayout.LayoutParams)chatLV.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_START);
			}else{
				((RelativeLayout.LayoutParams)chatLV.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				((RelativeLayout.LayoutParams)chatLV.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
			}
			((RelativeLayout.LayoutParams)chatLV.getLayoutParams()).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		}


		speaker.stop();
		speaker.shutdown();
		stopTexting();
	}
	private void stopTexting(){
		try{
			unregisterReceiver(intentReceiver);
		}catch (IllegalArgumentException e){}
	}
	public void setTrimmings(String input){
		int color = Color.parseColor(input.substring(0));
		rolesTV.setTextColor(color);
		roleTV.setTextColor(color);
		alliesTV.setTextColor(color);
		roleInfoTV.setTextColor(color);
		membersTV.setTextColor(color);
		commandTV.setTextColor(color);
	}

	public void updateRoleInfo(JSONObject role){
		roleTV.setText(JUtils.getString(role, StateObject.roleName));
		roleInfoTV.setText(JUtils.getString(role, StateObject.roleDescription));
	}


	public void showAllies(){
		if(manager.getCurrentPlayer() == null){
			hideView(alliesTV);
			hideView(alliesLV);
			hideView(roleInfoTV);
			hideView(roleTV);
		}else{
			showView(roleTV);
			showView(roleInfoTV);
			JSONObject roleInfo = manager.ns.getRoleInfo(manager.getCurrentPlayer());
			if(roleInfo.has(StateObject.roleTeam)){
				showView(alliesLV);
				if(wideMode()) {
					hideView(alliesTV);
				}else{
					hideView(rolesLV);
					hideView(rolesTV);
					showView(alliesTV);
				}
			} else{
				hideView(alliesTV);
				hideView(alliesLV);
			}
		}
	}


	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		ArrayList<String> checkedPlayers = getCheckedPlayers(0);
		if(checkedPlayers.isEmpty())
			return;
		manager.command(true, checkedPlayers);
	}

	public void onNothingSelected(AdapterView<?> arg0) {

	}

	public void setVotesToLynch(int votes){
		commandTV.setText("Number of votes to lynch - " + votes);
	}

	public void closeDrawer(){
		dayWindow.closeDrawer(playerMenu);
	}
	public void onDrawerClosed(View v){
		log("drawer closed");
		drawerOut = false;
		synchronized(manager.ns.local){
		manager.dScreenController.updatePlayerControlPanel();
		}
	}
	public void onDrawerOpened(View v){
		drawerOut = true;
	}
	public void onDrawerSlide(View v, float f){}
	public void onDrawerStateChanged(int i){}
	public void onDoubleTap() {
		if (!drawerOut && ns.isInProgress()) {
			synchronized(manager.ns.local){
				manager.nextSimulation();
			}
		}
	}
	private boolean drawerOut = false;
	public boolean drawerOut(){
		return drawerOut;
	}


	public boolean dispatchTouchEvent(@NonNull MotionEvent me){
		detector.onTouchEvent(me);
		return super.dispatchTouchEvent(me);
	}
	public void onSwipe(int direction) {
		if(direction == SimpleGestureFilter.SWIPE_LEFT){
			log("swiped left");
		}else if (direction == SimpleGestureFilter.SWIPE_RIGHT){
			log("swiped right");
		}
		manager.setNextAbility(direction);
	}

	public ArrayList<String> getCheckedPlayers(int column) {
		ArrayList<String> ret = new ArrayList<>();
		CheckBox cb;
		View v;
		TextView tv;
		for(int i = 0; i < targetablesAdapter.getCount(); i++){
			v = actionLV.getChildAt(i);
			cb = TargetablesAdapter.getCheckBox(v, column);
			if(cb.isChecked()){
				tv = (TextView) v.findViewById(R.id.target_name);
				ret.add(tv.getText().toString());
			}
		}
		return ret;
	}

	public void onDestroy(){
		unbindNarrator();
		speaker.stop();
		speaker.shutdown();
		try {
			unregisterReceiver(intentReceiver);
		}catch(IllegalArgumentException e){}
		super.onDestroy();
	}

	public void onAuthStateChanged(FirebaseAuth fa){
		super.nonHomeAuthChange(fa);
	}
}
