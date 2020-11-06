package com.cvdnn.classify;

import android.Args;
import android.frame.context.FrameActivity;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.cvdnn.classify.databinding.ActMainBinding;

import java.util.Arrays;

public abstract class UIActivity extends FrameActivity<ActMainBinding> {

    @Override
    protected final ActMainBinding onViewBinding() {
        return ActMainBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreateView() {
        super.onCreateView();

        binding.panelOperate.focusView.requestFocus();

        onCreateSpinnerView(binding.panelOperate.spCtl, Args.Env.Res.getStringArray(R.array.control_array));
        onCreateSpinnerView(binding.panelOperate.spSen, Args.Env.Res.getStringArray(R.array.sensor_array));
    }

    protected final String getSelectedItemText(@NonNull Spinner sp) {
        String text = "";
        Object item = sp.getSelectedItem();
        if (item instanceof String) {
            text = (String) item;
        }

        return text;
    }

    protected final void onCreateSpinnerView(Spinner sp, String[] itemArrays) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Arrays.asList(itemArrays));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    protected synchronized final void makeLogcat(String format, Object... args) {
        runOnUiThread(() -> binding.panelLogcat.tvLogcatText.setText(new StringBuilder()
                .append(String.format(format, args))
                .append("\n")
                .append(binding.panelLogcat.tvLogcatText.getText())
                .toString()));
    }
}
