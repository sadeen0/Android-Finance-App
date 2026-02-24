package com.labproject.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.database.Cursor;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.labproject.app.data.db.DBHelper;
import com.labproject.app.data.session.SessionManager;
import com.labproject.app.databinding.ActivityMainBinding;
import com.labproject.app.ui.Models.MainViewModel;
import com.labproject.app.ui.auth.LoginActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.labproject.app.data.prefs.SettingsManager;
public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SettingsManager settings = new SettingsManager(this);
        String theme = settings.getTheme();
        AppCompatDelegate.setDefaultNightMode(
                theme.equals("dark")
                        ? AppCompatDelegate.MODE_NIGHT_YES
                        : AppCompatDelegate.MODE_NIGHT_NO
        );

        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        super.onCreate(savedInstanceState);


        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        DBHelper db = new DBHelper(this);

        MainViewModel mainViewModel =
                new ViewModelProvider(this).get(MainViewModel.class);

        String email = session.getUserEmail();
        mainViewModel.loadTransactions(db, email);

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        // nav header user info
        View header = navigationView.getHeaderView(0);
        TextView tvName = header.findViewById(R.id.tvNavName);
        TextView tvEmail = header.findViewById(R.id.tvNavEmail);

        tvEmail.setText(email == null ? "" : email);

        if (email != null) {
            Cursor c = db.getUserByEmail(email);
            if (c != null && c.moveToFirst()) {
                String first = c.getString(0);
                String last = c.getString(1);
                tvName.setText(first + " " + last);
            } else {
                tvName.setText("User");
            }
            if (c != null) c.close();
        } else {
            tvName.setText("User");
        }

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home,
                R.id.nav_income,
                R.id.nav_expenses,
                R.id.nav_transactions,
                R.id.nav_budgets,
                R.id.nav_statistics,
                R.id.nav_profile,
                R.id.nav_settings
        ).setOpenableLayout(drawer).build();

        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Handle logout
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_logout) {
                session.logout();
                Intent i = new Intent(this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
                return true;
            } else {
                // Always navigate to the selected destination
                navController.navigate(itemId);
                drawer.close();
                return true;
            }
        });
    }

    public void refreshDrawerHeader() {
        SessionManager session = new SessionManager(this);
        DBHelper db = new DBHelper(this);

        NavigationView navigationView = binding.navView;
        View header = navigationView.getHeaderView(0);

        TextView tvName = header.findViewById(R.id.tvNavName);
        TextView tvEmail = header.findViewById(R.id.tvNavEmail);

        String email = session.getUserEmail();
        tvEmail.setText(email == null ? "" : email);

        if (email != null) {
            Cursor c = db.getUserByEmail(email);
            if (c != null && c.moveToFirst()) {
                String first = c.getString(0);
                String last = c.getString(1);
                tvName.setText(first + " " + last);
            } else {
                tvName.setText("User");
            }
            if (c != null) c.close();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        refreshDrawerHeader();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        SessionManager session = new SessionManager(this);

        int id = item.getItemId();

        if (id == R.id.action_statistics) {
            // Only navigate if not already on this destination
            if (navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.nav_statistics) {
                navController.navigate(R.id.nav_statistics);
            }
            return true;
        } else if (id == R.id.action_profile) {
            if (navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.nav_profile) {
                navController.navigate(R.id.nav_profile);
            }
            return true;
        } else if (id == R.id.action_settings) {
            if (navController.getCurrentDestination() != null && 
                navController.getCurrentDestination().getId() != R.id.nav_settings) {
                navController.navigate(R.id.nav_settings);
            }
            return true;
        } else if (id == R.id.action_logout) {
            session.logout();
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController =
                Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
