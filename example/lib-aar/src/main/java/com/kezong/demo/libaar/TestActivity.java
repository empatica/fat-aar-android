package com.kezong.demo.libaar;

import android.app.Activity;
import android.os.Bundle;

import com.kezong.demo.libaar.databinding.DatabindingBinding;

public class TestActivity extends Activity {

    private DatabindingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DatabindingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        User user = new User();
        user.setName("Hello World");
        user.setSex("[success][dataBinding] male");
        binding.name.setText(user.getName());
        binding.sex.setText(user.getSex());
    }
}
