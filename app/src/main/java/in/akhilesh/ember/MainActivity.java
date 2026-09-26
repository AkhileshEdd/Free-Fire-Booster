package in.akhilesh.ember;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Lightweight platform UI. No ads, telemetry, account, or always-running service. */
public class MainActivity extends Activity {
    private static final int BG = Color.rgb(16,18,16), CARD = Color.rgb(27,30,26), LINE = Color.rgb(49,54,46);
    private static final int INK = Color.rgb(241,243,233), MUTED = Color.rgb(164,174,158), AMBER = Color.rgb(255,181,128), GREEN = Color.rgb(190,222,153);
    private static final String[] GAMES = {"Free Fire", "Free Fire MAX"};
    private static final String[] PACKAGES = {"com.dts.freefireth", "com.dts.freefiremax"};
    private static final String[] PROFILES = {"Balanced", "Competitive", "Endurance"};
    private static final String[] TABS = {"Launch", "Network", "Profiles", "Journal", "More"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> networkTask;
    private volatile Socket currentSocket;
    private int generation;
    private Store store;
    private LinearLayout root, body;
    private int page = 0, game = 0;
    private boolean running;
    private String endpoint = "1.1.1.1", testedEndpoint = "", testedNetwork = "", progress = "";
    private long testedAt;
    private final List<Double> samples = new ArrayList<>();
    private TextView liveTimer;
    private final Runnable timer = new Runnable() {
        @Override public void run() {
            if (liveTimer != null && store.active().has("start")) liveTimer.setText(elapsed(store.active().optLong("start")));
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new Store(this);
        game = store.prefs.getInt("game", 0);
        if (game < 0 || game > 1) game = 0;
        if (state != null) page = state.getInt("page", 0);
        endpoint = store.get("endpoint", "1.1.1.1");
        render();
        if (!store.prefs.getBoolean("welcomed", false)) welcome();
    }
    @Override protected void onResume() { super.onResume(); if (store != null) render(); handler.removeCallbacks(timer); handler.post(timer); }
    @Override protected void onPause() { super.onPause(); handler.removeCallbacks(timer); if (running) cancelTest(); }
    @Override protected void onDestroy() { cancelTest(); executor.shutdownNow(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle b) { super.onSaveInstanceState(b); b.putInt("page", page); }
    private void welcome() {
        new AlertDialog.Builder(this).setTitle("A calmer way to get game-ready.")
            .setMessage("Ember brings your device checks, setup profiles and session journal together.\n\nIt does not change Free Fire files, unlock FPS, clean other apps or route game traffic. Profiles are notes you apply in the game.\n\nEverything stays on this phone. Optional network tests contact Cloudflare or Google only when you run them.")
            .setPositiveButton("Let's begin", (d,w) -> store.prefs.edit().putBoolean("welcomed", true).apply()).setCancelable(false).show();
    }
    public void navigate(int target) { if (running && target != 1) cancelTest(); page = target; render(); }
    private void render() {
        liveTimer = null;
        root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(i.left, i.top, i.right, i.bottom);
            } else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(root);
        LinearLayout top = row(); top.setPadding(dp(24),dp(14),dp(24),dp(12));
        TextView logo = text("ϟ  ember",24,INK,true); top.addView(logo, new LinearLayout.LayoutParams(0,-2,1));
        TextView badge = text("  PRO  ",10,GREEN,true); badge.setLetterSpacing(.15f); badge.setPadding(dp(10),dp(7),dp(10),dp(7)); badge.setBackground(shape(Color.rgb(41,51,33),14,0)); top.addView(badge);
        root.addView(top);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        body = column(); body.setPadding(dp(24),dp(12),dp(24),dp(24)); scroll.addView(body);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        switch(page) { case 1: network(); break; case 2: profiles(); break; case 3: journal(); break; case 4: more(); break; default: dashboard(); }
        LinearLayout nav = row(); nav.setPadding(dp(8),dp(8),dp(8),dp(8)); nav.setBackground(shape(BG,0,LINE));
        for (int i=0;i<TABS.length;i++) {
            final int index=i; TextView tab=text(TABS[i],12,i==page?AMBER:MUTED,i==page);
            tab.setGravity(Gravity.CENTER); tab.setMinHeight(dp(52)); tab.setId(100+i); tab.setContentDescription(TABS[i]+" tab");
            if(i==page) tab.setBackground(shape(Color.rgb(51,40,30),14,0));
            tab.setOnClickListener(v -> navigate(index)); nav.addView(tab,new LinearLayout.LayoutParams(0,-2,1));
        }
        root.addView(nav); root.requestApplyInsets();
    }
    private void header(String eyebrow, String title, String sub) {
        TextView e = text(eyebrow.toUpperCase(Locale.ROOT),10,GREEN,true); e.setLetterSpacing(.20f); add(e,0,10);
        add(text(title,32,INK,true),0,8); add(text(sub,14,MUTED,false),0,22);
    }
    private String profile() { return store.get("profile."+game,"Balanced"); }
    private void dashboard() {
        DeviceSnapshot d=DeviceSnapshot.read(this);
        header("YOUR PRE-MATCH RITUAL", "Good games start here.", "A little preparation. More time to play.");
        LinearLayout hero=card(); hero.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(49,59,37),CARD}));
        TextView tag=text("DEVICE CHECK",10,GREEN,true); tag.setLetterSpacing(.16f); hero.addView(tag);
        LinearLayout ready=row(); List<String> warnings=Metrics.warnings(d.battery,d.temperature,d.saver,d.connected,d.charging);
        Ring ring=new Ring(warnings.isEmpty()); ready.addView(ring,new LinearLayout.LayoutParams(dp(90),dp(90)));
        LinearLayout summary=column(); summary.setPadding(dp(18),0,0,0);
        summary.addView(text(warnings.isEmpty()?"Looking good":"Check before play",21,INK,true));
        summary.addView(text(warnings.isEmpty()?"No common flags detected":"A few things need a look",12,MUTED,false));
        ready.addView(summary,new LinearLayout.LayoutParams(0,-2,1)); hero.addView(ready);
        hero.addView(text("Battery, connection and power mode · checked now",11,MUTED,false)); add(hero,0,14);
        LinearLayout metrics=row(); metric(metrics,"BATTERY", d.battery<0?"—":d.battery+"%",d.charging?"Charging":"On battery"); metric(metrics,"BATTERY TEMP",d.temperature==null?"—":String.format(Locale.US,"%.1f°",d.temperature),"Not CPU temperature"); add(metrics,0,18);
        if (!warnings.isEmpty()) for (String warning:warnings) add(notice(warning),0,10);
        if (d.battery<0 || d.temperature==null) add(notice("Some battery readings are unavailable on this device. Readiness checks may be incomplete."),0,12);
        if (store.active().has("start")) { activeCard(); }
        section("Your game", "02 SUPPORTED");
        LinearLayout games=row();
        for(int i=0;i<2;i++) {
            final int g=i; Button b=button(GAMES[i]+(installed(i)?"":"\nNot installed"),i==game,()->{game=g;store.prefs.edit().putInt("game",g).apply();render();});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1); lp.setMargins(i==0?0:dp(8),0,0,0); games.addView(b,lp);
        } add(games,0,16);
        LinearLayout setup=card(); setup.addView(text(profile()+" setup",19,INK,true));
        setup.addView(text(profileDescription(profile()),13,MUTED,false));
        setup.addView(button("Edit setup  ↗",false,()->navigate(2))); add(setup,0,16);
        add(button(store.active().has("start")?"Return to game  ↗":"Prepare & launch  ↗",true,()->prepare(d)),0,8);
        add(text("No game-file changes. No performance promises.",11,MUTED,false),0,22);
        section("Device details", "LIVE SNAPSHOT");
        LinearLayout details=card(); item(details,"Connection",d.network); item(details,"Available RAM",gb(d.availableMemory)+" / "+gb(d.totalMemory));
        item(details,"Free storage",gb(d.freeStorage));
        float refresh=getWindowManager().getDefaultDisplay().getRefreshRate(); item(details,"Display refresh",Math.round(refresh)+" Hz · not game FPS");
        item(details,"Battery Saver",d.saver?"On":"Off"); add(details,0,8);
        add(button("Refresh readings",false,this::render),0,0);
    }
    private void activeCard() {
        JSONObject a=store.active(); LinearLayout c=card(); c.addView(text("SESSION IN PROGRESS",10,GREEN,true));
        c.addView(text(a.optString("game")+" · "+a.optString("profile"),17,INK,true));
        liveTimer=text(elapsed(a.optLong("start")),28,AMBER,true); c.addView(liveTimer);
        c.addView(text("Elapsed since launch; includes time away from the game. End it here when you're done.",12,MUTED,false));
        c.addView(button("End & review session",true,this::finishDialog)); add(c,0,18);
    }
    private boolean installed(int index) { return getPackageManager().getLaunchIntentForPackage(PACKAGES[index])!=null; }
    private void prepare(DeviceSnapshot ignored) {
        if(store.active().has("start")) {
            String activeGame=store.active().optString("game");
            int index=activeGame.equals(GAMES[1])?1:0;
            if(index!=game) { new AlertDialog.Builder(this).setTitle("Finish your current session")
                .setMessage("Your "+activeGame+" session is still open. End and review it before starting another game.")
                .setPositiveButton("Review session",(x,w)->finishDialog()).setNegativeButton("Cancel",null).show(); return; }
            launch(false); return;
        }
        if(!installed(game)) { new AlertDialog.Builder(this).setTitle(GAMES[game]+" isn't installed")
            .setMessage("Install the official game if it is available in your region. You can still use device checks, network tests and profiles.")
            .setPositiveButton("Open Play Store",(x,w)->open(new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id="+PACKAGES[game]))))
            .setNegativeButton("Not now",null).show(); return; }
        DeviceSnapshot d=DeviceSnapshot.read(this);
        LinearLayout content=column(); content.setPadding(dp(22),dp(8),dp(22),0);
        TextView guidance=text("Apply your saved settings inside the game. Starting a session only opens the game and records the start time and device readings.",14,MUTED,false); content.addView(guidance);
        content.addView(text("\n"+profile()+" · "+profileDescription(profile()),15,INK,true));
        for(String warning:Metrics.warnings(d.battery,d.temperature,d.saver,d.connected,d.charging)) content.addView(text("\n• "+warning,13,AMBER,false));
        content.addView(button("Review Do Not Disturb",false,()->settings("android.settings.ZEN_MODE_SETTINGS")));
        new AlertDialog.Builder(this).setTitle("Ready for "+GAMES[game]+"?").setView(content)
            .setPositiveButton("Launch game",(x,w)->launch(true)).setNegativeButton("Not yet",null).show();
    }
    private void launch(boolean start) {
        Intent intent=getPackageManager().getLaunchIntentForPackage(PACKAGES[game]);
        if(intent==null) { toast("Game isn't available on this device.");return; }
        try { startActivity(intent); if(start) store.start(GAMES[game],profile(),DeviceSnapshot.read(this)); }
        catch(ActivityNotFoundException|SecurityException e) { toast("Android couldn't open the game. Try opening it directly."); }
    }
    private void network() {
        header("CONNECTION LAB", "Know your connection.", "Measure consistency before your next match.");
        LinearLayout card=card(); card.addView(text("PUBLIC ENDPOINT",10,GREEN,true));
        RadioGroup group=new RadioGroup(this); group.setOrientation(LinearLayout.HORIZONTAL);
        for(String ip:new String[]{"1.1.1.1","8.8.8.8"}) {
            RadioButton r=new RadioButton(this); r.setText(ip.equals("1.1.1.1")?"Cloudflare":"Google"); r.setTextColor(INK); r.setTextSize(13); r.setMinHeight(dp(48)); r.setEnabled(!running);
            r.setButtonTintList(ColorStateList.valueOf(AMBER)); r.setChecked(endpoint.equals(ip));
            r.setOnClickListener(v->{endpoint=ip;store.put("endpoint",ip);}); group.addView(r,new RadioGroup.LayoutParams(0,-2,1));
        } card.addView(group);
        card.addView(text("12 TCP connection attempts on port 443. Measures a public endpoint, not Free Fire servers or in-game ping. Failed attempts are not a packet-loss measurement.",13,MUTED,false));
        card.addView(button(running?"Cancel test":"Run connection test",true,()->{if(running){cancelTest();render();}else consentTest();})); add(card,0,18);
        if(!samples.isEmpty() || running) {
            Metrics.NetworkResult result=new Metrics.NetworkResult(samples);
            add(text(running?progress:"Last test · "+testedEndpoint,14,GREEN,true),0,8);
            if(testedAt>0) add(text(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(testedAt))+" · "+testedNetwork,12,MUTED,false),0,12);
            LinearLayout stats=row(); metric(stats,"MEDIAN",ms(result.median),"TCP connect");metric(stats,"VARIATION",ms(result.variation),"Adjacent samples");add(stats,0,16);
            LinearLayout graph=card(); graph.addView(new Trace(samples),new LinearLayout.LayoutParams(-1,dp(140)));
            graph.addView(text(samples.size()+" / 12 attempts · "+result.failures+" failed",12,MUTED,false)); add(graph,0,12);
            add(notice(running?"Keep Ember open until the test completes.":progress.startsWith("Canceled")?progress:result.median==null?"No successful connections. Check internet access or try the other endpoint. A blocked test does not prove the game is unreachable.":"Compare tests on Wi-Fi and mobile data. Lower variation can indicate a steadier connection to this endpoint; game results can differ."),0,16);
            if(samples.size()>0) {
                StringBuilder raw=new StringBuilder();for(int i=0;i<samples.size();i++) raw.append(i+1).append(": ").append(samples.get(i)==null?"failed":ms(samples.get(i))).append(i==samples.size()-1?"":"    ");
                add(text(raw.toString(),12,MUTED,false),0,12);
            }
        } else {
            LinearLayout empty=card();empty.addView(text("A clearer picture, in seconds.",20,INK,true));empty.addView(text("Your real measurements will appear here. No simulated scores or invented latency.",14,MUTED,false));add(empty,0,16);
        }
        section("Small changes worth trying", "CONNECTION");
        add(notice("Pause large downloads. Move closer to your router. Compare Wi-Fi with mobile data rather than assuming one is faster."),0,12);
    }
    private void consentTest() {
        if(!DeviceSnapshot.read(this).connected) {toast("Connect to the internet before testing.");return;}
        new AlertDialog.Builder(this).setTitle("Run a public connection test?")
            .setMessage("Ember will open 12 short TCP connections to "+endpoint+":443. The endpoint provider can see your IP address and connection metadata. No game traffic, profile or session history is sent.\n\nThis test doesn't change your connection.")
            .setPositiveButton("Run test",(d,w)->startTest()).setNegativeButton("Cancel",null).show();
    }
    private void startTest() {
        running=true;samples.clear();testedEndpoint=endpoint;testedNetwork=DeviceSnapshot.read(this).network;testedAt=System.currentTimeMillis();progress="Testing 0 / 12";
        final int token=++generation; final String ip=endpoint;render();
        networkTask=executor.submit(()->{
            for(int i=0;i<12&&!Thread.currentThread().isInterrupted();i++) {
                Double sample=null;
                try(Socket socket=new Socket()) {currentSocket=socket;long start=System.nanoTime();socket.connect(new InetSocketAddress(ip,443),1800);sample=(System.nanoTime()-start)/1_000_000.0;}
                catch(Exception ignored) { } finally {currentSocket=null;}
                if(Thread.currentThread().isInterrupted())return;
                final Double value=sample;final int count=i+1;
                handler.post(()->{if(token!=generation||isDestroyed())return;samples.add(value);progress="Testing "+count+" / 12";if(count==12){running=false;progress="Complete";}if(page==1)render();});
                if(i<11)try{Thread.sleep(250);}catch(InterruptedException e){Thread.currentThread().interrupt();return;}
            }
        });
    }
    private void cancelTest() {
        if(!running)return;running=false;generation++;progress="Canceled · partial results only";
        if(networkTask!=null)networkTask.cancel(true);
        try{Socket s=currentSocket;if(s!=null)s.close();}catch(Exception ignored){}
    }
    private void profiles() {
        header("YOUR PLAYBOOK", "Find your sweet spot.", "A saved setup for the way you like to play.");
        LinearLayout gamePicker=row();for(int i=0;i<2;i++){final int g=i;Button b=button(GAMES[i],game==i,()->{game=g;store.prefs.edit().putInt("game",g).apply();render();});gamePicker.addView(b,new LinearLayout.LayoutParams(0,-2,1));}add(gamePicker,0,16);
        for(String p:PROFILES) {
            LinearLayout c=card(); if(p.equals(profile()))c.setBackground(shape(Color.rgb(45,51,36),20,GREEN));
            c.addView(text(p+(p.equals(profile())?"  ✓":""),20,p.equals(profile())?GREEN:INK,true));
            c.addView(text(profileDescription(p),14,MUTED,false));
            c.addView(button(p.equals(profile())?"Selected":"Use this setup",false,()->{store.put("profile."+game,p);render();}));add(c,0,12);
        }
        add(notice("These are starting points, not device-tested presets. Apply settings manually in Free Fire. Available options depend on the game version and phone."),0,18);
        section("Personal notes", "SAVED ON DEVICE");
        LinearLayout notes=card();String key="notes."+game+"."+profile();
        notes.addView(text(store.get(key,"Save sensitivity values, HUD layout, graphics choices or anything you want to remember."),14,INK,false));
        notes.addView(button("Edit setup notes",false,()->editNotes(key)));add(notes,0,16);
        section("Pre-match checklist", "YOUR ROUTINE");
        LinearLayout check=card();String[] checks={"Applied graphics settings in game","Checked sound and microphone","Reviewed notifications / Do Not Disturb","Paused downloads and updates"};
        for(int i=0;i<checks.length;i++){CheckBox b=new CheckBox(this);b.setText(checks[i]);b.setTextColor(INK);b.setTextSize(14);b.setMinHeight(dp(48));b.setButtonTintList(ColorStateList.valueOf(GREEN));final String k="check."+game+"."+i;b.setChecked(store.prefs.getBoolean(k,false));b.setOnCheckedChangeListener((v,yes)->store.prefs.edit().putBoolean(k,yes).apply());check.addView(b);}
        check.addView(button("Reset checklist",false,()->{for(int i=0;i<checks.length;i++)store.prefs.edit().remove("check."+game+"."+i).apply();render();}));add(check,0,0);
    }
    private static String profileDescription(String p) {
        if(p.equals("Competitive"))return "Start with Smooth graphics. Try a higher frame-rate option only if your phone stays stable. Reduce visual effects where available.";
        if(p.equals("Endurance"))return "Start with Smooth graphics and a standard frame rate. Lower brightness manually to a comfortable level. Take breaks if the phone gets warm.";
        return "Start with Standard graphics and the default frame rate. If you see stutters or heat, lower graphics first and compare another session.";
    }
    private void editNotes(String key) {
        EditText input=new EditText(this);input.setText(store.get(key,""));input.setHint("Graphics, sensitivity, HUD layout…");input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);input.setMinLines(4);input.setMaxLines(8);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1000)});
        new AlertDialog.Builder(this).setTitle(GAMES[game]+" · "+profile()).setView(input).setPositiveButton("Save notes",(d,w)->{store.put(key,input.getText().toString());render();}).setNegativeButton("Cancel",null).show();
    }
    private void journal() {
        header("SESSION JOURNAL", "Learn from every match.", "Your setups. Your observations. All on your phone.");
        if(store.active().has("start"))activeCard();
        JSONArray sessions=store.sessions();
        if(sessions.length()==0){LinearLayout c=card();c.addView(text("Your first session starts a story.",23,INK,true));c.addView(text("Launch a game from Ember, then return to end the session. Record how it felt and compare your setups over time.",14,MUTED,false));c.addView(button("Go to launcher",true,()->navigate(0)));add(c,0,16);return;}
        long total=0;for(int i=0;i<sessions.length();i++){JSONObject s=sessions.optJSONObject(i);if(s!=null)total+=Metrics.duration(s.optLong("start"),s.optLong("end"));}
        LinearLayout stats=row();metric(stats,"SESSIONS",""+sessions.length(),"Latest 200 retained");metric(stats,"ELAPSED",duration(total),"Includes time away");add(stats,0,18);
        add(button("Export journal as CSV",false,this::export),0,18);
        for(int i=0;i<sessions.length();i++) {
            JSONObject s=sessions.optJSONObject(i);if(s==null)continue;LinearLayout c=card();
            c.addView(text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(s.optLong("start"))),11,MUTED,false));
            c.addView(text(s.optString("game"),21,INK,true));c.addView(text(s.optString("profile")+" · "+duration(Metrics.duration(s.optLong("start"),s.optLong("end")))+" · "+s.optString("rating"),13,GREEN,false));
            item(c,"Battery",battery(s.optInt("batteryStart",-1))+" → "+battery(s.optInt("batteryEnd",-1)));
            item(c,"Battery temperature",temp(s,"temperatureStart")+" → "+temp(s,"temperatureEnd"));
            if(!s.optString("note").isEmpty())c.addView(text(s.optString("note"),14,INK,false));add(c,0,12);
        }
        add(text("Readings are taken at start and end only. Changes include charging, other apps and time away. They do not measure FPS or prove a setup improved performance.",12,MUTED,false),0,0);
    }
    private void finishDialog() {
        if(!store.active().has("start"))return;
        LinearLayout content=column();content.setPadding(dp(24),0,dp(24),0);
        content.addView(text("How did the session feel?",16,INK,true));Spinner rating=new Spinner(this);rating.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"Not rated","Smooth","Mixed","Stuttery"}));content.addView(rating);
        EditText note=new EditText(this);note.setHint("What would you change next time?");note.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);note.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});content.addView(note);
        new AlertDialog.Builder(this).setTitle("Save your session").setView(content).setPositiveButton("Save session",(d,w)->{store.finish(DeviceSnapshot.read(this),rating.getSelectedItem().toString(),note.getText().toString());navigate(3);}).setNegativeButton("Keep running",null).show();
    }
    private void export() {
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("text/csv").putExtra(Intent.EXTRA_TITLE,"ember-sessions.csv");
        try{startActivityForResult(i,501);}catch(ActivityNotFoundException e){toast("No document picker is available.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==501&&result==RESULT_OK&&data!=null&&data.getData()!=null){
        try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new java.io.IOException();out.write(store.exportCsv().getBytes(StandardCharsets.UTF_8));toast("Journal exported.");}catch(Exception e){toast("Export failed. Try another destination.");}
    }}
    private void more() {
        header("MADE FOR YOUR ROUTINE", "Less noise. More play.", "No ads. No subscription. No account to create.");
        LinearLayout c=card();c.addView(text("System shortcuts",20,INK,true));c.addView(text("You stay in control. Ember opens Android settings; it doesn't silently change them.",13,MUTED,false));
        c.addView(button("Do Not Disturb  ↗",false,()->settings("android.settings.ZEN_MODE_SETTINGS")));
        c.addView(button("Wi-Fi settings  ↗",false,()->settings(Settings.ACTION_WIFI_SETTINGS)));
        c.addView(button("Display & brightness  ↗",false,()->settings(Settings.ACTION_DISPLAY_SETTINGS)));
        c.addView(button("Battery Saver  ↗",false,()->settings(Settings.ACTION_BATTERY_SAVER_SETTINGS)));add(c,0,18);
        LinearLayout privacy=card();privacy.addView(text("Private by design",20,GREEN,true));privacy.addView(text("Profiles and up to 200 sessions are stored locally. No analytics, ads, tracking SDKs or cloud account. Android cloud backup is disabled. CSV exports go only where you choose.",14,MUTED,false));
        privacy.addView(button("Read privacy details",false,()->new AlertDialog.Builder(this).setTitle("Privacy in Ember").setMessage("Ember stores your selected game, setup notes, checklist, session times, start/end battery readings and your session feedback on this device.\n\nOnly an explicitly started network test contacts a third party: Cloudflare (1.1.1.1) or Google (8.8.8.8), port 443. They receive your IP address and connection metadata under their policies. Results remain in memory until this activity is closed.\n\nOpening a game or a settings screen hands control to that app. Opening Play Store follows Google's policies. Exported files remain in your chosen location until you delete them.\n\nErase local data below or uninstall to remove Ember's stored records. No developer-operated server receives them.").setPositiveButton("Close",null).show()));add(privacy,0,18);
        LinearLayout about=card();about.addView(text("Honest tools. Real readings.",20,INK,true));about.addView(text("Ember cannot unlock FPS, cool hardware, clean another app's memory, change Free Fire graphics or guarantee lower ping. It is a preparation and diagnostics toolkit.\n\nFree Fire and Free Fire MAX belong to their respective owners. Ember is independent and is not affiliated with or endorsed by Garena.",14,MUTED,false));add(about,0,18);
        add(button("Erase all local data",false,()->new AlertDialog.Builder(this).setTitle("Erase Ember data?").setMessage("This permanently removes all profiles, notes, sessions and the active timer from this phone. Exported CSV files are not deleted.").setNegativeButton("Keep data",null).setPositiveButton("Erase data",(d,w)->{store.prefs.edit().clear().commit();game=0;samples.clear();testedAt=0;endpoint="1.1.1.1";navigate(0);welcome();}).show()),0,16);
        add(text("EMBER  /  1.0.0\nDesigned for a more intentional game session.",11,MUTED,false),0,0);
    }
    private void settings(String action){
        try { startActivity(new Intent(action)); }
        catch(ActivityNotFoundException|SecurityException e) {
            if(action.equals("android.settings.ZEN_MODE_SETTINGS")) open(new Intent(Settings.ACTION_SOUND_SETTINGS));
            else toast("This settings screen isn't available on your phone.");
        }
    }
    private void open(Intent i){try{startActivity(i);}catch(ActivityNotFoundException|SecurityException e){toast("This screen isn't available on your phone.");}}
    private void toast(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
    private String elapsed(long start){return duration(Metrics.duration(start,System.currentTimeMillis()));}
    private static String duration(long ms){long seconds=ms/1000;return String.format(Locale.US,"%02d:%02d:%02d",seconds/3600,(seconds%3600)/60,seconds%60);}
    private static String gb(long b){return String.format(Locale.US,"%.1f GB",b/1073741824.0);}
    private static String ms(Double value){return value==null?"—":String.format(Locale.US,"%.0f ms",value);}
    private static String battery(int b){return b<0?"—":b+"%";}
    private static String temp(JSONObject o,String key){return o.isNull(key)?"—":String.format(Locale.US,"%.1f°C",o.optDouble(key));}
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private LinearLayout card(){LinearLayout l=column();l.setPadding(dp(18),dp(18),dp(18),dp(18));l.setBackground(shape(CARD,20,LINE));return l;}
    private GradientDrawable shape(int color,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setFontFeatureSettings("kern");t.setLineSpacing(dp(3),1f);if(bold)t.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));return t;}
    private void add(View v,int top,int bottom){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(top),0,dp(bottom));body.addView(v,lp);}
    private Button button(String title,boolean primary,Runnable action){Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setTextColor(primary?BG:INK);b.setMinHeight(dp(52));b.setPadding(dp(14),dp(10),dp(14),dp(10));b.setBackgroundTintList(null);
        android.graphics.drawable.RippleDrawable ripple=new android.graphics.drawable.RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),shape(primary?AMBER:Color.rgb(38,42,35),14,primary?0:LINE),null);b.setBackground(ripple);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(10),0,0);b.setLayoutParams(lp);b.setOnClickListener(v->action.run());return b;}
    private TextView notice(String value){TextView n=text(value,13,MUTED,false);n.setPadding(dp(16),dp(14),dp(16),dp(14));n.setBackground(shape(Color.rgb(29,34,27),14,LINE));return n;}
    private void metric(LinearLayout parent,String title,String value,String hint){LinearLayout c=card();c.addView(text(title,9,MUTED,true));c.addView(text(value,27,INK,true));c.addView(text(hint,10,MUTED,false));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-1,1);lp.setMargins(parent.getChildCount()==0?0:dp(10),0,0,0);parent.addView(c,lp);}
    private void section(String title,String label){LinearLayout r=row();r.addView(text(title,18,INK,true),new LinearLayout.LayoutParams(0,-2,1));r.addView(text(label,9,MUTED,true));add(r,6,12);}
    private void item(LinearLayout c,String title,String value){LinearLayout r=row();r.setPadding(0,dp(9),0,dp(9));r.addView(text(title,12,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));TextView v=text(value,12,INK,true);v.setGravity(Gravity.END);r.addView(v,new LinearLayout.LayoutParams(0,-2,1));c.addView(r);}
    private final class Ring extends View {
        final boolean ready;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Ring(boolean ready){super(MainActivity.this);this.ready=ready;setContentDescription(ready?"No common readiness flags":"Readiness checks need attention");}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float x=getWidth()/2f,y=getHeight()/2f,r=dp(34);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(3));p.setColor(LINE);c.drawCircle(x,y,r,p);p.setColor(ready?GREEN:AMBER);c.drawArc(x-r,y-r,x+r,y+r,-90,ready?360:270,false,p);p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(dp(28));c.drawText(ready?"✓":"!",x,y+dp(10),p);}
    }
    private final class Trace extends View {
        final List<Double> data;final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        Trace(List<Double> data){super(MainActivity.this);this.data=new ArrayList<>(data);setContentDescription("TCP connection time bars. Raw readings listed below.");}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight()-dp(24);double max=10;for(Double v:data)if(v!=null)max=Math.max(max,v);float step=w/12f;
            p.setStrokeWidth(dp(1));p.setColor(LINE);for(int i=1;i<=3;i++)c.drawLine(0,h*i/3,w,h*i/3,p);
            for(int i=0;i<data.size();i++){Double v=data.get(i);p.setColor(v==null?AMBER:GREEN);float bh=v==null?dp(4):Math.max(dp(4),(float)(v/max)*(h-dp(12)));c.drawRoundRect(i*step+dp(3),h-bh,(i+1)*step-dp(3),h,dp(3),dp(3),p);}
            p.setTextSize(dp(10));p.setColor(MUTED);p.setTextAlign(Paint.Align.LEFT);c.drawText("0",0,getHeight()-dp(5),p);p.setTextAlign(Paint.Align.RIGHT);c.drawText("Scale max "+Math.round(max)+" ms",w,getHeight()-dp(5),p);}
    }
}
