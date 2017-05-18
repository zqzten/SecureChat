package com.akikosoft.securechat;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private SecureChatApplication app;
    private DrawerLayout mDrawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
        }

        app = (SecureChatApplication) getApplication();
        app.bindSocketEvents();
        app.connectSocket();
        app.emitOnline();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
        }

        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        View header = navView.getHeaderView(0);
        TextView email = (TextView) header.findViewById(R.id.email);
        email.setText(app.userEmail);

        final Menu navMenu = navView.getMenu();
        navView.setCheckedItem(R.id.nav_chat);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // logout
                if (item.getItemId() == R.id.nav_logout) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(R.string.logout);
                    builder.setMessage(R.string.logout_alert_msg);
                    builder.setPositiveButton(R.string.logout, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                            startActivity(intent);
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            navView.setCheckedItem(R.id.nav_chat);
                            navMenu.performIdentifierAction(R.id.nav_chat, 0);
                        }
                    });
                    builder.show();
                    return true;
                }

                // switch fragment
                Fragment fragment = null;
                String tag = null;
                switch (item.getItemId()) {
                    case R.id.nav_chat:
                        fragment = new ChatFragment();
                        tag = "chat";
                        break;
                    case R.id.nav_friends:
                        fragment = new FriendsFragment();
                        tag = "friends";
                        break;
                }
                FragmentManager manager = getSupportFragmentManager();
                manager.beginTransaction().replace(R.id.content_frame, fragment, tag).commit();
                if (actionBar != null) {
                    actionBar.setTitle(item.getTitle());
                }
                mDrawerLayout.closeDrawers();
                return true;
            }
        });
        navMenu.performIdentifierAction(R.id.nav_chat, 0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        app.mainActivity = this;
    }

    @Override
    protected void onStop() {
        super.onStop();
        app.mainActivity = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        app.emitOffline();
        app.unbindSocketEvents();
        app.initUserInfo();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.add_friend:
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    public void notifyChatItemAppended() {
        ChatFragment chatFragment = (ChatFragment) getSupportFragmentManager().findFragmentByTag("chat");
        if (chatFragment != null) chatFragment.notifyItemAppended();
    }

    public void notifyChatItemChanged(int position) {
        ChatFragment chatFragment = (ChatFragment) getSupportFragmentManager().findFragmentByTag("chat");
        if (chatFragment != null) chatFragment.notifyItemChanged(position);
    }

    public void notifyChatItemRemoved(int position) {
        ChatFragment chatFragment = (ChatFragment) getSupportFragmentManager().findFragmentByTag("chat");
        if (chatFragment != null) chatFragment.notifyItemRemoved(position);
    }

    public void notifyFriendsChanged() {
        FriendsFragment friendsFragment = (FriendsFragment) getSupportFragmentManager().findFragmentByTag("friends");
        if (friendsFragment != null) friendsFragment.notifyChanged();
    }

}
