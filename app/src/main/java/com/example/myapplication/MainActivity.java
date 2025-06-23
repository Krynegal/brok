package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.example.myapplication.Models.Asset;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Body;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> resultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        String rawToken = prefs.getString("token", null);
        if (rawToken == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        final String token = "Bearer " + rawToken;

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

        TextView totalAssetsSum = findViewById(R.id.textViewAssetsSum);

        RecyclerView recyclerView = findViewById(R.id.recyclerViewAssets);

        List<Asset> assetList = new ArrayList<>();

        assetList.add(new Asset(getString(R.string.broker_account_1), "type1", 100.0));

        assetList.add(new Asset(getString(R.string.broker_account_2), "type2", 200.0));

        recyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        // Создаём обработчик для получения данных из нового Activity
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Когда возвращаемся из нового Activity, обрабатываем полученные данные
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String assetName = result.getData().getStringExtra("assetName");
                        String assetValue = result.getData().getStringExtra("assetValue");
                        //String assetProfit = result.getData().getStringExtra("assetProfit");

                        int position = result.getData().getIntExtra("position", -1);
                        updateItemInAdapter(new Asset(assetName, assetValue, 0.0), position);

                        //updateTotalSumTextView(totalAssetsSum);
                        updateTotalSumTextView();
                    }
                }
        );

        MyAdapter adapter = new MyAdapter(assetList, new MyAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // Действия при клике на элемент
                Asset asset = assetList.get(position);


                Intent intent = new Intent(MainActivity.this, AssetLobbyActivity.class);

                intent.putExtra("position", position);
                intent.putExtra("assetName", asset.getName());
                intent.putExtra("assetBalance", asset.getBalance());
                //intent.putExtra("assetProfit", asset.getProfit());

                resultLauncher.launch(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        // Загрузка активов из API
        AssetApiService api = ApiClient.getClient().create(AssetApiService.class);
        api.getAssets(token).enqueue(new Callback<List<Asset>>() {
            @Override
            public void onResponse(Call<List<Asset>> call, Response<List<Asset>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    assetList.clear();
                    assetList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    updateTotalSumTextView();
                } else {
                    Toast.makeText(MainActivity.this, "Ошибка загрузки активов", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Asset>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });

        binding.buttonAddAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog(adapter, api, token, assetList);
            }
        });
    }

    // Метод для обновления данных в адаптере
    private void updateItemInAdapter(Asset asset, int position) {
        if (position != -1) {
            // Обновляем данные в адаптере
            MyAdapter adapter = (MyAdapter) ((RecyclerView) findViewById(R.id.recyclerViewAssets)).getAdapter();
            if (adapter != null) {
                adapter.updateAsset(position, asset);
            }
        }
    }

    //private void updateTotalSumTextView(TextView totalAssetsSum) {
    private void updateTotalSumTextView() {
        MyAdapter adapter = (MyAdapter) ((RecyclerView) findViewById(R.id.recyclerViewAssets)).getAdapter();

        if (adapter != null) {
            Double totalSum = adapter.getTotalSum();

            TextView totalAssetsSum = findViewById(R.id.textViewAssetsSum);

            totalAssetsSum.setText(String.valueOf(totalSum));
        }
    }

    private void showInputDialog(MyAdapter adapter, AssetApiService api, String token, List<Asset> assetList) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_new_asset));
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText assetNameInput = new EditText(this);
        final EditText assetTypeInput = new EditText(this);
        assetNameInput.setHint(getString(R.string.asset_name_hint));
        assetTypeInput.setHint("Тип актива");
        dialogLayout.addView(assetNameInput);
        dialogLayout.addView(assetTypeInput);
        builder.setView(dialogLayout);
        builder.setPositiveButton(getString(R.string.done), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userInputAssetName = assetNameInput.getText().toString();
                String userInputAssetType = assetTypeInput.getText().toString();
                if (userInputAssetName.isEmpty() || userInputAssetType.isEmpty()) {
                    Toast.makeText(MainActivity.this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                    return;
                }
                CreateAssetRequest req = new CreateAssetRequest();
                req.name = userInputAssetName;
                req.type = userInputAssetType;
                api.createAsset(token, req).enqueue(new Callback<Asset>() {
                    @Override
                    public void onResponse(Call<Asset> call, Response<Asset> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            Log.d("CREATE ASSET", "Asset created: " + response.body().toString());
                            assetList.add(response.body());
                            adapter.notifyItemInserted(assetList.size() - 1);
                            updateTotalSumTextView();
                            Toast.makeText(MainActivity.this, "Актив добавлен", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Ошибка добавления актива", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Asset> call, Throwable t) {
                        Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
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