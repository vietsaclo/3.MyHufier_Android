package com.nhom08.doanlaptrinhandroid;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.os.Handler;
import android.speech.RecognizerIntent;
import android.view.SubMenu;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import android.view.MenuItem;
import com.google.android.material.navigation.NavigationView;
import com.nhom08.doanlaptrinhandroid.BLL.Wp_post_BLL;
import com.nhom08.doanlaptrinhandroid.BLL.Wp_term_BLL;
import com.nhom08.doanlaptrinhandroid.DTO.Wp_post;
import com.nhom08.doanlaptrinhandroid.DTO.Wp_term;
import com.nhom08.doanlaptrinhandroid.DTO.Wp_user;
import com.nhom08.doanlaptrinhandroid.Interface_enum.KindStatusRunning;
import com.nhom08.doanlaptrinhandroid.Interface_enum.OnMyFinishListener;
import com.nhom08.doanlaptrinhandroid.Interface_enum.OnMyUpdateProgress;
import com.nhom08.doanlaptrinhandroid.Modulds.FunctionsStatic;
import com.nhom08.doanlaptrinhandroid.adapter.Wp_postRecyclerViewAdapter;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.Menu;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/*
    06/2020
        Activity chính hiển thị các nội dung bài viết và các danh mục trên menu.
    Khi mới khỏi động Activity sẽ gọi một Activity là loading để hiển thị cho người dùng
    biết là ứng dụng đang tải dữ liệu, bởi vì việc tải dữ liệu là lấy từ internet.
    khi tải xong dữ liệu. Activity Loading sẽ tự động đóng và hiển thị các danh mục và bài
    viết.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static final int REQUEST_SEARCH_BY_VOICE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //region # khởi tạo ngôn ngữ mặc định(vi),& theme được lấy từ SharedPreferences
        loadLocale();
        checkTheme();
        //endregion

        super.onCreate(savedInstanceState);

        //region khởi tạo view và khỏi tạo menu
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        //endregion

        //region chuẩn bị đầy đủ các dữ liệu và load dữ liệu
        init();
        //endregion
    }

    /*
        khởi tạo các biến bổ sung và load dữ liệu vào các listView, Recycleview
     */
    private void init(){
        //Loading xong chưa?
        notifyLoadingData();

        //region khỏi tạo biến
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setColorSchemeColors(Color.BLUE, Color.RED, Color.GREEN);
        swipeRefreshLayout.setOnRefreshListener(swipeRefreshLayoutOnRefresh);
        isRefreshing = false;

        mediaPlayerNewPostRing = MediaPlayer.create(MainActivity.this, R.raw.new_posts_ring);
        fabNotify = findViewById(R.id.fabNotify);
        fabUser = findViewById(R.id.fab);
        fabNotify.setOnClickListener(fabNotifyClicked);
        fabUser.setOnClickListener(fabClicked);

        doChenhLenh = 0;
        coCapNhapTuChuong = true;
        postsHienTaiChuaCapNhat = new ArrayList<>();

        recyLoadBaiViet = findViewById(R.id.recycler_show_wp_post);
        recyLoadBaiViet.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0){
                    fabUser.hide();
                    fabNotify.hide();
                }else{
                    fabUser.show();
                    if (doChenhLenh != 0)
                        fabNotify.show();
                }
            }
        });

        wp_post_bll = new Wp_post_BLL();
        wp_term_bll = new Wp_term_BLL();

        processDialog = FunctionsStatic.createProcessDialog(MainActivity.this);
        //endregion

        //user đã login?
        getUserWasLogin();

        //region tải dữ liệu cho menu và listView
        updateRecyclerView(getString(R.string.url_wp_posts));
        NavigationView navigationView = findViewById(R.id.nav_view);
        loadMenuNavigationView(navigationView.getMenu());
        //endregion

        dividerItemDecoration = new DividerItemDecoration(this, DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(getDrawable(R.drawable.divider)));
    }

    /*
        function: loadMenuNavigationView
        params:
        - menu: truyền vào menu của Drawer mặt định để khi có dữ liệu
        sẽ tự động load động và add vào menu đó.

        #load menu thực hiện trên một luồng khác khi xong sẽ gọi lại MainActivity
     */
    private void loadMenuNavigationView(final Menu menu){
        if(!isRefreshing)
            processDialog.show();
        menu.clear();
        wp_term_bll.toArrayWp_terms(getString(R.string.url_wp_terms), new OnMyFinishListener<ArrayList<Wp_term>>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish(ArrayList<Wp_term> result) {
                menu.add(0, 42,0,R.string.chu_module_for_dev).setIcon(R.drawable.ic_widgets_black_24dp);
                SubMenu subMenu1 = menu.addSubMenu(getString(R.string.top_category));
                menuItemChecked = subMenu1.add(1, 0, 0, getString(R.string.new_posts)).setIcon(R.drawable.ic_menu_news_post).setChecked(true);
                //For other category
                ArrayList<Wp_term> otherCates = new ArrayList<>();

                //Thêm vào mặc định các thể loại
                for(Wp_term term : result) {
                    int id = term.getTerm_id();
                    String name = term.getName();
                    switch (id){
                        case 42:break;//module for dev was added

                        case 3: {
                            MenuItem menuItem = subMenu1.add(1, id, 0, name);
                            menuItem.setIcon(R.drawable.ic_school_black_24dp);
                            break;
                        }
                        case 4: {
                            MenuItem menuItem = subMenu1.add(1, id, 0, name);
                            menuItem.setIcon(R.drawable.ic_menu_khoa_hoc);
                            break;
                        }
                        case 5: {
                            MenuItem menuItem = subMenu1.add(1, id, 0, name);
                            menuItem.setIcon(R.drawable.ic_menu_0xdhth);
                            break;
                        }
                        case 30: {
                            MenuItem menuItem = subMenu1.add(1, id, 0, name);
                            menuItem.setIcon(R.drawable.ic_menu_source_code);
                            break;
                        }
                        default: {
                            otherCates.add(term);
                            break;
                        }
                    }
                }

                //thêm vào các thể loại khác nếu tìm thấy
                SubMenu subMenu2 = menu.addSubMenu(getString(R.string.other_category));
                for(Wp_term term : otherCates){
                    int id = term.getTerm_id();
                    String name = term.getName();
                    if (id == 1)
                        subMenu2.add(2, id, 0, name.toUpperCase()).setIcon(R.drawable.ic_menu_not_category);
                    else
                        subMenu2.add(2, id, 0, name).setIcon(R.drawable.ic_menu_category);
                }

                SubMenu subMenu3 = menu.addSubMenu(getString(R.string.app_tools));

                //User đã login?
                if (userWasLogin != null){//user đã login!
                    subMenu3.add(3, R.string.navActionLogout, 0, getString(R.string.chu_dang_xuat)).setIcon(R.drawable.ic_menu_logout);

                    TextView tvNavTitle = findViewById(R.id.tvNavActonBarTitle),
                            tvNavContent = findViewById(R.id.tvNavActionBarContent);
                    tvNavTitle.setText(getString(R.string._Username)+userWasLogin.getUser_login());
                    tvNavContent.setText(getString(R.string._myhufier)+userWasLogin.getDisplay_name());
                }
                else{//user chưa login!
                    subMenu3.add(3, R.string.navActionLogin, 0, getString(R.string.chu_dang_nhap)).setIcon(R.drawable.ic_menu_login);
                    subMenu3.add(3, R.string.navActionRegister, 0, getString(R.string.chu_dang_ky)).setIcon(R.drawable.ic_menu_register);

                    TextView tvNavTitle = findViewById(R.id.tvNavActonBarTitle),
                            tvNavContent = findViewById(R.id.tvNavActionBarContent);
                    tvNavTitle.setText(R.string.welcomeToTitle);
                    tvNavContent.setText(R.string.welcomeToContent);
                }

                subMenu3.add(3, R.string.chart, 0, getString(R.string.chart) ).setIcon(R.drawable.img_chart);

                FunctionsStatic.cancelDialog(processDialog);
                cancelSwipeRefreshLayout();

                DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
                drawerLayout.openDrawer(GravityCompat.START);

                //Đã tải xong dữ liêu! phá hủy Activity loading
                notifyLoadingDataFinish();
            }

            @Override
            public void onError(Throwable error, Object bonusOfCoder) {
                FunctionsStatic.cancelDialog(processDialog);
                cancelSwipeRefreshLayout();
                Toast.makeText(MainActivity.this, "LOAD MENU ERROR: Unknown Error!"+error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.app_name));
        searchView = (SearchView)menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setOnSearchClickListener(searchViewClicked);
        searchView.setOnQueryTextListener(searchViewQuerySubmit);

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
            Intent intent = new Intent(MainActivity.this, Setting.class);
            startActivity(intent);
        }else if (id == 1){
            showLanguagesDialog();
        }else if (id == R.id.mnuSearchVoice){
            setupSearchByVoice();
        }
        else if (id == R.id.action_changeLayout){
            int layout = getLayoutRecycleView();
            SharedPreferences.Editor editor = getSharedPreferences("myHufierSetting", MODE_PRIVATE).edit();
            if (layout == 1) {
                //recyLoadBaiViet.removeItemDecoration(dividerItemDecoration);
                editor.putInt("layout", 2);
            }
            else {
                //recyLoadBaiViet.addItemDecoration(dividerItemDecoration);
                editor.putInt("layout", 1);
            }
            editor.apply();
            updateRecyclerView(postsHienTaiChuaCapNhat);
        }

        return super.onOptionsItemSelected(item);
    }

    private int getLayoutRecycleView(){
        SharedPreferences sharedPreferences = getSharedPreferences("myHufierSetting", MODE_PRIVATE);
        int layout = sharedPreferences.getInt("layout", 1);
        return layout;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id < 0)
            return false;

        if(id == R.string.navActionLogout){
            SharedPreferences sp = getSharedPreferences("myHufierLogin", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.clear();
            editor.apply();
            userWasLogin = null;
            NavigationView navigationView = findViewById(R.id.nav_view);
            loadMenuNavigationView(navigationView.getMenu());
            return true;
        }

        if (id == R.string.navActionLogin){
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            return true;
        }

        if (id == R.string.navActionRegister){
            Intent intent = new Intent(this, Register.class);
            startActivity(intent);
            return true;
        }
        if (id == R.string.chart){
            Intent intent = new Intent(this, Chart.class);
            startActivity(intent);
            return true;
        }

        if (id == 42) {//module for dev
            Intent intent = new Intent(MainActivity.this, ModuleForDev.class);
            intent.putExtra("term_id", 42);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        }

        if (menuItemChecked != null)
            menuItemChecked.setChecked(false);

        item.setChecked(true);
        menuItemChecked = item;

        if (id == 0) {
            coCapNhapTuChuong = true;
            updateRecyclerView(getString(R.string.url_wp_posts));
        }
        else
            updateRecyclerView(getString(R.string.url_wp_posts_term) + id);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    /*
        # Function: updateRecyclerView
        # params: String
        # thực hiện cập nhật RecyclerView khi truyền vào một link API.
        thực hiện công việc băng một luồng khác và khi thực hiện xong sẽ gọi lại
        MainActivity
     */
    private void updateRecyclerView(String strUrl){
        if(!isRefreshing)
            processDialog.show();
        wp_post_bll.toArrayWp_posts(strUrl, new OnMyFinishListener<ArrayList<Wp_post>>() {
            @Override
            public void onFinish(ArrayList<Wp_post> result) {
                updateRecyclerView(result);
                processDialog.cancel();
                cancelSwipeRefreshLayout();
            }

            @Override
            public void onError(Throwable error, Object bonusOfCoder) {
                processDialog.cancel();
                cancelSwipeRefreshLayout();
            }
        });
    }

    /*
        Overloading: updateRecyclerView
        Params: danh sách bài viết đã có dữ liệu
     */
    private void updateRecyclerView(final List<Wp_post> list){
        if (postsHienTaiChuaCapNhat.size() == 0 || coCapNhapTuChuong) {
            postsHienTaiChuaCapNhat.clear();
            postsHienTaiChuaCapNhat.addAll(list);
            fabNotify.hide();
            doChenhLenh = 0;
            coCapNhapTuChuong = false;//cn
        }

        wp_postAdapter = new Wp_postRecyclerViewAdapter(list);
        recyLoadBaiViet.setAdapter(wp_postAdapter);
        recyLoadBaiViet.setHasFixedSize(true);
        recyLoadBaiViet.setLayoutManager(new LinearLayoutManager(this));

//        int layout = getLayoutRecycleView();
//        if (layout == 1)
//            recyLoadBaiViet.addItemDecoration(dividerItemDecoration);
//        else
//            recyLoadBaiViet.removeItemDecoration(dividerItemDecoration);
    }

    /*
        hàm trợ giúp.
        Trả về một user nếu đã đăng nhập trước đó.
        null nếu chưa đăng nhập.
     */
    private void getUserWasLogin(){
        SharedPreferences sp = getSharedPreferences("myHufierLogin", MODE_PRIVATE);
        String uName = sp.getString("uName", null),
                fName = sp.getString("uFName", null),
                pass = sp.getString("uPass", null),
                email = sp.getString("uEmail", null),
                uid = sp.getString("uID", null),
                uUrl = sp.getString("uUrl", null);

        if (uName != null && fName != null && pass != null){
            userWasLogin = new Wp_user();
            userWasLogin.setUser_login(uName);
            userWasLogin.setDisplay_name(fName);
            userWasLogin.setUser_pass(pass);
            userWasLogin.setUser_email(email);
            try{
                userWasLogin.setID(Integer.parseInt(uid));
            }catch (NumberFormatException n){
                Toast.makeText(this, n.getMessage(), Toast.LENGTH_SHORT).show();
                userWasLogin.setID(-1);
            }
            userWasLogin.setUser_url(uUrl);
        }
        else
            userWasLogin = null;
    }

    /*
        sự kiện khi làm mới bằng swipeRefresh
     */
    private SwipeRefreshLayout.OnRefreshListener swipeRefreshLayoutOnRefresh = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            isRefreshing = true;
            if(menuItemChecked == null) {
                NavigationView navigationView = findViewById(R.id.nav_view);
                loadMenuNavigationView(navigationView.getMenu());
                updateRecyclerView(getString(R.string.url_wp_posts));
            }else {
                onNavigationItemSelected(menuItemChecked);
            }
        }
    };

    /*
        hàm trợ giúp
     */
    private void cancelSwipeRefreshLayout(){
        if(isRefreshing)
            swipeRefreshLayout.setRefreshing(false);
        isRefreshing = !isRefreshing;
    }

    /*
        đang tải dữ liệu nè!
        hiển thị form loading lên.
     */
    private void notifyLoadingData(){
        Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                //Notify Loading.. data;
                SharedPreferences sp = getSharedPreferences("@myHufierLoading", MODE_PRIVATE);
                SharedPreferences.Editor editor = sp.edit();
                editor.putString("status", KindStatusRunning.LOADING_DATA.toString());
                editor.apply();

                Intent intent = new Intent(MainActivity.this, ScreenWaitLogo.class);
                startActivity(intent);
            }
        });
    }

    /*
        gởi một thông báo đến form Loading.
        loading sẽ nhận lại nếu thấy đã xong sẽ tự phá hủy.
     */
    private void notifyLoadingDataFinish(){
        //Notify Loading.. data finish;
        SharedPreferences sp = getSharedPreferences("@myHufierLoading", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("status", KindStatusRunning.FINISH_LOADING_DATA.toString());
        editor.apply();
        //setUpThongBaoKhiCoBaiViet(20000);
    }

    /*
        Lọc dữ liệu
     */
    private SearchView.OnQueryTextListener searchViewQuerySubmit = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {

            return false;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            wp_postAdapter.filter(newText);
            return true;
        }
    };

    /*
        tìm kiếm dữ liệu trên database
     */
    private View.OnClickListener searchViewClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(R.string.chu_thong_bao);
            builder.setMessage(R.string.chu_ban_muon_loc_hay_tim_kim);
            builder.setPositiveButton(R.string.chu_loc, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    searchView.setQueryHint(getString(R.string.chu_nhap_de_loc));
                }
            });
            builder.setNegativeButton(R.string.chu_tim_bat_cu_dau, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    searchView.clearFocus();
                    AlertDialog.Builder builderSearch = new AlertDialog.Builder(MainActivity.this);
                    builderSearch.setTitle(R.string.chu_tim_bat_cu_dau);
                    builderSearch.setView(MainActivity.this.getLayoutInflater().inflate(R.layout.layout_search_anywhere, null));
                    dialog.cancel();
                    showSearchViewPopup(builderSearch.create());
                }
            });
            builder.create().show();
        }
    };

    /*
        hàm trợ giúp cho sự kiện: searchViewClicked
     */
    private void showSearchViewPopup(final AlertDialog alertDialog){
        alertDialog.setCancelable(false);
        alertDialog.show();

        //get view
        final EditText edtEnterKey = alertDialog.findViewById(R.id.edtEnterKeyToSearch);
        SeekBar seekBarDoChinhXac = alertDialog.findViewById(R.id.seekBarDoChinhXacSearchAnywhere);
        final TextView tvDoChinhXac = alertDialog.findViewById(R.id.tvChonDoChinhXacSearchAnywhere);
        final ProgressBar progressBarSumPosts = alertDialog.findViewById(R.id.progressBarTinhToanSumPosts);
        final ProgressBar progressBarLoading = alertDialog.findViewById(R.id.progressBarSearchAnywhere);
        final TextView tvTinhTong = alertDialog.findViewById(R.id.tvSumPostsSearchAnywhere);
        final TextView tvLoading = alertDialog.findViewById(R.id.tvLoadingPercentSearchAnywhere);
        final TextView tvTimThay = alertDialog.findViewById(R.id.tvTimThayKetQuaSearchAnyWhere);
        Button btnCancel = alertDialog.findViewById(R.id.btnCancelSeachAnyWhere);
        Button btnSearch = alertDialog.findViewById(R.id.btnTimKiemSearchAnywhere);

        //init component.
        final int MAX_SEEKBAR = 4;
        final int[] DO_CHINH_XAC = {MAX_SEEKBAR * 100 / MAX_SEEKBAR};
        seekBarDoChinhXac.setMax(MAX_SEEKBAR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            seekBarDoChinhXac.setProgress(MAX_SEEKBAR, true);
        }else
            seekBarDoChinhXac.setProgress(4);

        tvDoChinhXac.setText(getString(R.string.chon_do_chinh_xac));

        progressBarSumPosts.setVisibility(View.GONE);

        tvLoading.setText(getString(R.string.loading_not));

        tvTimThay.setText(getString(R.string.tim_thay_0));

        //set seekbar change progress.
        seekBarDoChinhXac.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                DO_CHINH_XAC[0] = progress * 100 / MAX_SEEKBAR;
                tvDoChinhXac.setText(String.format(getString(R.string.chon_do_chinh_xac_percent), DO_CHINH_XAC[0]));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //set btnClicked
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
                searchView.clearFocus();
            }
        });

        final boolean[] isBusy = {false};
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBusy[0]) {//is busy?
                    Toast.makeText(MainActivity.this, R.string.chu_tieng_trinh_dang_ban, Toast.LENGTH_SHORT).show();
                    return;
                }

                //key empty?
                final String keySearch = edtEnterKey.getText().toString();
                if (keySearch.isEmpty()){
                    edtEnterKey.setError(getString(R.string.chu_cho_nay_khac_rong));
                    edtEnterKey.requestFocus();
                    return;
                }

                //show process sum posts.
                isBusy[0] = true;
                progressBarSumPosts.setVisibility(View.VISIBLE);
                wp_post_bll.toArrayWp_posts(getString(R.string.url_wp_posts), new OnMyFinishListener<ArrayList<Wp_post>>() {
                    @Override
                    public void onFinish(ArrayList<Wp_post> result) {
                        //finish sum posts;
                        int sumPost = result.size();
                        progressBarSumPosts.setVisibility(View.GONE);
                        tvTinhTong.setText(String.format(getString(R.string.trang_thai_tong_posts_items), sumPost));
                        progressBarLoading.setMax(sumPost);
                        progressBarLoading.setProgress(0);
                        tvTimThay.setText(getString(R.string.tim_thay_0));

                        //Find Items By Key In background.
                        wp_post_bll.findItems(result, keySearch, DO_CHINH_XAC[0], new OnMyFinishListener<ArrayList<Wp_post>>() {
                            @Override
                            public void onFinish(ArrayList<Wp_post> result) {
                                updateRecyclerView(result);
                                isBusy[0] = false;
                            }

                            @Override
                            public void onError(Throwable error, Object bonusOfCoder) {
                                Toast.makeText(MainActivity.this, "Error: " + bonusOfCoder.toString(), Toast.LENGTH_SHORT).show();
                                isBusy[0] = false;
                            }
                        }, new OnMyUpdateProgress<Integer, Integer>() {
                            @Override
                            public void onUpdateProgress(final Integer percent) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBarLoading.setProgress(percent+1);
                                        tvLoading.setText(String.format(getString(R.string.loading_percent), percent+1));
                                    }
                                });
                            }

                            @Override
                            public void onUpdateValue(final Integer integer) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        tvTimThay.setText(getString(R.string.tim_thay)+integer);
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable error, Object bonusOfCoder) {
                        Toast.makeText(MainActivity.this, "Error: "+bonusOfCoder.toString(), Toast.LENGTH_SHORT).show();
                        isBusy[0] = false;
                    }
                });
            }
        });
    }

    //Hiển thị tùy chọn ngôn ngữ
    private void showLanguagesDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chose Language...");
        builder.setSingleChoiceItems(new String[]{"Vietnamese", "English"}, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0){
                    setLocale("vi");
                }else{
                    setLocale("");
                }
                recreate();
                dialog.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void setLocale(String lang){
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration conf = getResources().getConfiguration();
        conf.setLocale(locale);
        getResources().updateConfiguration(conf, getResources().getDisplayMetrics());
    }

    private void loadLocale(){
        //get setting shared từ key: myHufierSetting
        SharedPreferences sp = getSharedPreferences("myHufierSetting", MODE_PRIVATE);
        String lang = sp.getString("myHufierLang", "vi");
        //postNumOld = sp.getInt("myHufierPostNum", 50);
        setLocale(lang);
    }

    /*
        Dành cho sự kiện fab
     */
    private View.OnClickListener fabNotifyClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            coCapNhapTuChuong = true;
            updateRecyclerView(getString(R.string.url_wp_posts));
        }
    };

    private View.OnClickListener fabClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.chu_chon_tac_vu));
            builder.setSingleChoiceItems(new String[]{getString(R.string.chu_dang_bai_moi), getString(R.string.chu_ve_nha_cua_toi)}, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (userWasLogin == null){
                        Toast.makeText(MainActivity.this, R.string.chu_can_dang_nhap_de_tiep_tuc, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    switch (which){
                        case 0:{
                            //Hiển thị Activity Đăng bài viết mới
                            Intent intent = new Intent(MainActivity.this, DangBaiMoi.class);
                            startActivity(intent);
                            break;
                        }
                        case 1:{
                            //Hiển thị Activity về nhà của user.
                            Intent intent = new Intent(MainActivity.this, UserHome.class);
                            startActivity(intent);
                            break;
                        }
                        default:break;
                    }
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }
    };


    //region # tu dong cap nhat bai viet
    /*Cứ sau 1 khoảng thời gian milisecond giấy thì tiến hành get all bài viết về so sánh với list bài
     * viết hiện tại. Nếu khác về số lượng bài thì
     * thông báo số bài viết mới cho người dùng.*/
    private void setUpThongBaoKhiCoBaiViet(final int milisecond) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (postsHienTaiChuaCapNhat == null)
                    return;

                wp_post_bll.toArrayWp_posts(getString(R.string.url_wp_posts), new OnMyFinishListener<ArrayList<Wp_post>>() {
                    @Override
                    public void onFinish(ArrayList<Wp_post> result) {
                        final int chenhLech = result.size() - postsHienTaiChuaCapNhat.size();
                        runOnUiThread(new Runnable() {
                            @SuppressLint("DefaultLocale")
                            @Override
                            public void run() {
                                try {
                                    if (doChenhLenh < chenhLech){
                                        fabNotify.hide();
                                        if (doChenhLenh <= 9)
                                            fabNotify.setImageBitmap(BitmapFactory.decodeStream(getAssets().open(String.format("icon/bell/ic_bell_notify_0%d.png", chenhLech))));
                                        else
                                            fabNotify.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("icon/bell/ic_bell_notify_99.png")));

                                        if (!mediaPlayerNewPostRing.isPlaying())
                                            mediaPlayerNewPostRing.start();
                                        fabNotify.show();
                                        doChenhLenh = chenhLech;
                                    }
                                }catch (IOException ioex){
                                    Toast.makeText(MainActivity.this, ioex.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(Throwable error, Object bonusOfCoder) {
                        Toast.makeText(MainActivity.this, "Lỗi thông báo bài viết mới!", Toast.LENGTH_SHORT).show();
                    }
                });
                handler.postDelayed(this, milisecond);
            }
        }, milisecond);
    }
    //endregion

    //region # search by voice
    /*
        Setup search by voice
     */
    private void setupSearchByVoice(){
        try{
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi|en-US");
            startActivityForResult(intent, REQUEST_SEARCH_BY_VOICE);
        }catch (Exception ex){
            FunctionsStatic.hienThiThongBaoDialog(this, getString(R.string.chu_thong_bao), getString(R.string.chu_thiet_bi_khong_ho_tro_am_thanh));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SEARCH_BY_VOICE
        && resultCode == RESULT_OK
        && data != null){
            final ArrayList<String> strings = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            assert strings != null;

            //do in background find by voice
            FunctionsStatic.showDialog(processDialog);
            final String keySearch = strings.get(0);
            wp_post_bll.toArrayWp_posts(getString(R.string.url_wp_posts), new OnMyFinishListener<ArrayList<Wp_post>>() {
                @Override
                public void onFinish(final ArrayList<Wp_post> posts) {
                    wp_post_bll.findItems(posts, keySearch, 50, new OnMyFinishListener<ArrayList<Wp_post>>() {
                        @Override
                        public void onFinish(ArrayList<Wp_post> result) {
                            FunctionsStatic.cancelDialog(processDialog);
                            FunctionsStatic.hienThiThongBaoDialog(MainActivity.this, getString(R.string.chu_thong_bao), String.format(getString(R.string.chu_da_tim_thay_d_bai_viet_tren_tong_d_bai_viet), result.size(), posts.size()));
                            updateRecyclerView(result);
                        }

                        @Override
                        public void onError(Throwable error, Object bonusOfCoder) {
                            FunctionsStatic.cancelDialog(processDialog);
                            FunctionsStatic.hienThiThongBaoDialog(MainActivity.this, getString(R.string.chu_thong_bao), getString(R.string.chu_loi_ket_noi_toi_internet));
                        }
                    }, new OnMyUpdateProgress<Integer, Integer>() {
                        @Override
                        public void onUpdateProgress(Integer percent) {

                        }

                        @Override
                        public void onUpdateValue(Integer integer) {

                        }
                    });
                }

                @Override
                public void onError(Throwable error, Object bonusOfCoder) {

                }
            });
        }
    }

    //endregion

    /*
        User setting dark mode?
     */
    private void checkTheme(){
        //lấy cài đặt từ SharedPrefrences từ key: myHufierSetting
        SharedPreferences sharedPreferences = getSharedPreferences("myHufierSetting", MODE_PRIVATE);
        //isDark?
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        if (isDarkMode)
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);//set Dark
        else
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);//set light

    }

    private Wp_post_BLL wp_post_bll;
    private Wp_term_BLL wp_term_bll;

    private AlertDialog processDialog;
    private MenuItem menuItemChecked;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private boolean isRefreshing;
    private SearchView searchView;
    private Wp_postRecyclerViewAdapter wp_postAdapter;
    private Wp_user userWasLogin;
    private FloatingActionButton fabNotify, fabUser;
    private RecyclerView recyLoadBaiViet;//set

    DividerItemDecoration dividerItemDecoration;

    private MediaPlayer mediaPlayerNewPostRing;
    private int doChenhLenh;
    private boolean coCapNhapTuChuong;//khi bấm chuông cờ = true. lúc này cập nhập lại postHienTaiChuaCapNhat
    private List<Wp_post> postsHienTaiChuaCapNhat;//danh sách thể hiện các bài viết chưa cập nhật. nếu có bài viết mới sẽ lấy danh sách này để đối chiếu.

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }
}