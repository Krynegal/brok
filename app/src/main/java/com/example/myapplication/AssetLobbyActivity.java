package com.example.myapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.w3c.dom.Text;

public class AssetLobbyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_asset_lobby);

        Button btnAddAsset = findViewById(R.id.button2);

        btnAddAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Создание операции");

        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText sumInput = new EditText(this);

        sumInput.setHint("Сумма");

        final TextView operationName = new TextView(this);
        final Spinner spinner = new Spinner(this);

        String[] operations = { "Ввод средств", "Вывод средств", "Изменение стоимости", "Дивиденты"};

        // Создаем адаптер ArrayAdapter с помощью массива строк и стандартной разметки элемета spinner
        ArrayAdapter<String> adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, operations);
        // Определяем разметку для использования при выборе элемента
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Применяем адаптер к элементу spinner
        spinner.setAdapter(adapter);

        dialogLayout.addView(sumInput);
        dialogLayout.addView(spinner);

        builder.setView(dialogLayout);

        AdapterView.OnItemSelectedListener itemSelectedListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                // Получаем выбранный объект
                String item = (String)parent.getItemAtPosition(position);
                operationName.setText(item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        spinner.setOnItemSelectedListener(itemSelectedListener);

        LinearLayout myRoot = findViewById(R.id.assetsLogsLayout);

        builder.setPositiveButton("Готово", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String userInputSum = sumInput.getText().toString(); // TODO валидация

                Toast.makeText(AssetLobbyActivity.this, "Создана операция: " + operationName.getText() + " на сумму " + userInputSum, Toast.LENGTH_SHORT).show();

                // Layout для одного лога
                LinearLayout a = new LinearLayout(AssetLobbyActivity.this);
                a.setOrientation(LinearLayout.VERTICAL);

                TextView assetLog = new TextView(AssetLobbyActivity.this);

                assetLog.setText(String.format("%s\t$%s", operationName.getText(), userInputSum));

                a.addView(assetLog);
                // Layout для одного лога

                myRoot.addView(a, 0);

                // Обновляем LinearLayout
                myRoot.requestLayout();
                myRoot.invalidate();
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
}
