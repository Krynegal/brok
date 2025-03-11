package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.example.myapplication.Models.Asset;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //setSupportActionBar(binding.toolbar);

//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
//        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

//        binding.buttonAddAsset.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAnchorView(R.id.buttonAddAsset)
//                        .setAction("Action", null).show();
//            }
//        });

        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        List<Asset> assetList = new ArrayList<>();

        assetList.add(new Asset("Брокерский счет 1", "123",
                "0", "10"));

        assetList.add(new Asset("Брокерский счет 2", "456",
                "100", "20"));

        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        MyAdapter adapter = new MyAdapter(assetList, new MyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Действия при клике на элемент
                Asset asset = assetList.get(position);

                startActivity(new Intent(MainActivity.this, AssetLobbyActivity.class));
            }
        });
        recyclerView.setAdapter(adapter);

        binding.buttonAddAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog(adapter);
            }
        });
    }

    private void showInputDialog(MyAdapter adapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Добавить новый актив");

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText assetNameInput = new EditText(this);
        final EditText startBalanceInput = new EditText(this);

        assetNameInput.setHint("Название актива");
        startBalanceInput.setHint("Стартовый баланс");

        dialogLayout.addView(assetNameInput);
        dialogLayout.addView(startBalanceInput);

        builder.setView(dialogLayout);

        //LinearLayout myRoot = findViewById(R.id.assetsLayout);

        builder.setPositiveButton("Готово", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userInputAssetName = assetNameInput.getText().toString();
                String userInputStartBalance = startBalanceInput.getText().toString(); // TODO валидация

                Toast.makeText(MainActivity.this, "Добавлен актив: " + userInputAssetName + "со стартовым балансом " + userInputStartBalance, Toast.LENGTH_SHORT).show();

                adapter.addAsset(new Asset(userInputAssetName, userInputStartBalance,
                        "0", "0"));

//                // Layout для одного лога
//                LinearLayout a = new LinearLayout(MainActivity.this);
//                a.setOrientation(LinearLayout.VERTICAL);
//
//                TextView assetLog = new TextView(MainActivity.this);
//
//                assetLog.setText(String.format("%s\t$%s", operationName.getText(), userInputSum));
//
//                a.addView(assetLog);
//                // Layout для одного лога
//
//                myRoot.addView(a, 0);
//
//                // Обновляем LinearLayout
//                myRoot.requestLayout();
//                myRoot.invalidate();
            }
        });

        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public boolean onSupportNavigateUp() {
//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
//        return NavigationUI.navigateUp(navController, appBarConfiguration)
//                || super.onSupportNavigateUp();
//    }
}