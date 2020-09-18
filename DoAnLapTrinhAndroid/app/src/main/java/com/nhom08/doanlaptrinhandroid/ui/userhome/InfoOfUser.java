package com.nhom08.doanlaptrinhandroid.ui.userhome;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

public class InfoOfUser extends Fragment {

    public static Fragment newInstance() {
        InfoOfUser infoOfUser = new InfoOfUser();
        return infoOfUser;
    }

    EditText edtId, edtUsername, edtFullName, edtEmail, edtURL, edtPass;
    Button btnLuu, btnThayDoiMK;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragmemt_info_of_user, container, false);
        final Wp_user userWasLogin = FunctionsStatic.newInstance().getUserWasLogin(root.getContext());

        edtId = root.findViewById(R.id.edtID);
        edtUsername = root.findViewById(R.id.edtUsername);
        edtFullName = root.findViewById(R.id.edtFullName);
        edtEmail = root.findViewById(R.id.edtEmail);
        edtURL = root.findViewById(R.id.edtUrl);
        btnLuu = root.findViewById(R.id.btnSave);
        btnThayDoiMK = root.findViewById(R.id.btnThayDoiMatKhau);

        edtId.setText(String.format("%d", userWasLogin.getID()));
        edtUsername.setText(String.format("%s", userWasLogin.getUser_login()));
        edtFullName.setText(String.format("%s", userWasLogin.getDisplay_name()));
        edtEmail.setText(String.format("%s", userWasLogin.getUser_email()));
        edtURL.setText(String.format("%s", userWasLogin.getUser_url() == null || userWasLogin.getUser_url().isEmpty() ? "Not Found" : userWasLogin.getUser_url()));

        edtId.setOnLongClickListener(suKienLongClicked_TuChoiUpdate);
        edtUsername.setOnLongClickListener(suKienLongClicked_TuChoiUpdate);
        edtEmail.setOnLongClickListener(suKienLongClicked_TuChoiUpdate);

        edtFullName.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                edtFullName.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                return false;
            }
        });

        edtURL.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                edtURL.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                return false;
            }
        });

        btnLuu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Intent intent = new Intent(v.getContext(), ThayDoiThongTinCaNhan.class);
                startActivity(intent);
            }
        });

        btnThayDoiMK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ChangePassword_ByInfoUser.class);
                intent.putExtra("user_email_ByInfoUser", userWasLogin.getUser_email());
                intent.putExtra("user_pass_old", userWasLogin.getUser_pass());
                startActivity(intent);

            }
        });
        return root;
    }

    public View.OnLongClickListener suKienLongClicked_TuChoiUpdate = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            FunctionsStatic.hienThiThongBaoDialog(v.getContext(), "Thông báo", "Thuộc tính này không thể sửa được!!");
            return false;
        }
    };
}
