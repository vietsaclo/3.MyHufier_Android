package com.nhom08.doanlaptrinhandroid.ui.userhome;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.nhom08.doanlaptrinhandroid.DTO.Wp_user;
import com.nhom08.doanlaptrinhandroid.Modulds.FunctionsStatic;
import com.nhom08.doanlaptrinhandroid.R;

import java.util.HashMap;
import java.util.Map;

public class ThayDoiThongTinCaNhan extends AppCompatActivity {

    EditText edtFullName, edtURL;
    Button btnHuy, btnLuu;
    ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thay_doi_thong_tin_ca_nhan);
        getSupportActionBar().hide();
        final Wp_user userWasLogin = FunctionsStatic.newInstance().getUserWasLogin(this);
        init();

        edtFullName.setText(String.format("%s", userWasLogin.getDisplay_name()));
        edtURL.setText(String.format("%s", userWasLogin.getUser_url() == null || userWasLogin.getUser_url().isEmpty() ? "Not Found" : userWasLogin.getUser_url()));

        btnHuy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThayDoiThongTinCaNhan.this.finish();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ThayDoiThongTinCaNhan.this.finish();
            }
        });

        btnLuu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                //region update
                if (userWasLogin == null)
                    return;
                final String fullname = edtFullName.getText().toString();
                final String userurl = edtURL.getText().toString();

                RequestQueue requestQueue = Volley.newRequestQueue(v.getContext());
                StringRequest stringRequest = new StringRequest(Request.Method.POST, getString(R.string.url_update_user_byid),
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {

                                SharedPreferences sp = v.getContext().getSharedPreferences("myHufierLogin", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sp.edit();
                                editor.clear();
                                editor.putString("uID", String.valueOf(userWasLogin.getID()));
                                editor.putString("uEmail", userWasLogin.getUser_email());
                                editor.putString("uName", userWasLogin.getUser_login());
                                editor.putString("uFName", fullname);
                                editor.putString("uPass", userWasLogin.getUser_pass());
                                editor.putString("uUrl", userurl);
                                editor.apply();
                                FunctionsStatic.hienThiThongBaoDialog(v.getContext(), "Thông báo", "Vui lòng restart lại để thấy thay đổi");
                                ThayDoiThongTinCaNhan.this.finish();
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Toast.makeText(v.getContext(), "Loi Update User", Toast.LENGTH_SHORT).show();
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() throws AuthFailureError {
                        Map<String, String> map = new HashMap<>();
                        map.put("user_id", userWasLogin.getID() + "");
                        map.put("user_fullname", fullname);
                        map.put("user_url", userurl);
                        return map;
                    }
                };
                requestQueue.add(stringRequest);
                //endregion
            }
        });
    }

    public void init()
    {
        btnLuu = findViewById(R.id.btnLuu);
        btnHuy = findViewById(R.id.btnHuy);
        btnBack = findViewById(R.id.imgBack);

        edtFullName = findViewById(R.id.edtFullName);
        edtURL = findViewById(R.id.edtUrl);
    }
}