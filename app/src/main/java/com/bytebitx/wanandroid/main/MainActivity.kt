package com.bytebitx.wanandroid.main

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.alibaba.android.arouter.facade.Postcard
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.facade.callback.NavigationCallback
import com.alibaba.android.arouter.launcher.ARouter
import com.bytebitx.base.base.BaseActivity
import com.bytebitx.base.constants.Constants
import com.bytebitx.base.constants.RouterPath
import com.bytebitx.base.ext.Prefs
import com.bytebitx.base.ext.Resource
import com.bytebitx.base.ext.observe
import com.bytebitx.base.ext.showToast
import com.bytebitx.base.util.AppUtil
import com.bytebitx.base.util.DialogUtil
import com.bytebitx.service.login.LoginOutService
import com.bytebitx.wanandroid.R
import com.bytebitx.wanandroid.databinding.ActivityMainBinding
import com.bytebitx.wanandroid.databinding.NavHeaderMainBinding
import com.google.android.material.navigation.NavigationBarView
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

@Route(path = RouterPath.Main.PAGE_MAIN)
@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var navHeaderBinding: NavHeaderMainBinding

    @Autowired(name = RouterPath.LoginRegister.SERVICE_LOGOUT)
    lateinit var loginOutService: LoginOutService

    //默认为0
    private var mIndex = 0
    //退出时间
    private var mExitTime: Long = 0

    private var homeFragment: Fragment? = null
    private var weChatFragment: Fragment? = null
    private var sysFragment: Fragment? = null
    private var squareFragment: Fragment? = null
    private var projectFragment: Fragment? = null

    private val mDialog by lazy {
        DialogUtil.getWaitDialog(this, getString(R.string.login_ing))
    }

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            mIndex = savedInstanceState.getInt("currTabIndex")
        }
        super.onCreate(savedInstanceState)
        observe()
    }

    override fun initView() {
        ARouter.getInstance().inject(this)
        initBus()

        navHeaderBinding = NavHeaderMainBinding.inflate(layoutInflater)
        binding.navView.addHeaderView(navHeaderBinding.root)
        binding.bottomNavigation.selectedItemId = mIndex
        binding.bottomNavigation.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        switchFragment(mIndex)

        binding.actionBar.run {
            title = getString(R.string.app_name)
            setSupportActionBar(this.toolbar)
        }

        binding.bottomNavigation.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.action_home ->
                    switchFragment(Constants.FragmentIndex.HOME_INDEX)
                R.id.action_wechat ->
                    switchFragment(Constants.FragmentIndex.WECHAT_INDEX)
                R.id.action_system ->
                    switchFragment(Constants.FragmentIndex.SYS_INDEX)
                R.id.action_square ->
                    switchFragment(Constants.FragmentIndex.SQUARE_INDEX)
                R.id.action_project ->
                    switchFragment(Constants.FragmentIndex.PROJECT_INDEX)
            }
            true
        }

        val toggle = ActionBarDrawerToggle(this, binding.drawerLayout, binding.actionBar.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        setUserInfo()

        navHeaderBinding.root.setOnClickListener {
            if (AppUtil.isLogin) {
                return@setOnClickListener
            }
            ARouter.getInstance().build(RouterPath.LoginRegister.PAGE_LOGIN).navigation()
        }

        binding.floatingBtn.setOnClickListener(onFABClickListener)

    }

    override fun initObserver() {
    }

    override fun initData() {
    }

    private fun observe() {
        observe(mainViewModel.logOutLiveData, ::handleLogOut)
    }

    private fun initBus() {
//        LiveDataBus.get().with(BusKey.LOGIN_SUCCESS, Any::class.java).observe(this) {
//            val userId = Prefs.getString(Constants.USER_ID)
//            val userName = Prefs.getString(Constants.USER_NAME)
//            if (userId.isEmpty()) return@observe
//            navHeaderBinding.userIdLayout.visibility = View.VISIBLE
//            navHeaderBinding.tvUserId.text = userId
//            navHeaderBinding.tvUsername.text = userName
//            binding.navView.menu.findItem(R.id.nav_logout).title = getString(R.string.nav_logout)
//        }
    }

    private fun handleLogOut(status: Resource<String>) {
        when (status) {
            is Resource.Loading -> {
                mDialog.show()
            }
            is Resource.Error -> {
                mDialog.dismiss()
                showToast(status.exception.stackTraceToString())
            }
            else -> {
                mDialog.dismiss()
                showToast(getString(R.string.logout_success))
                navHeaderBinding.tvUsername.text = getString(R.string.go_login)
                navHeaderBinding.userIdLayout.visibility = View.GONE
                navHeaderBinding.tvUserGrade.text = getString(R.string.nav_line_2)
                navHeaderBinding.tvUserRank.text = getString(R.string.nav_line_2)

                Prefs.clear()
                binding.navView.menu.findItem(R.id.nav_logout).title = getString(R.string.login)
                AppUtil.isLogin = false
            }
        }
    }



    private fun setUserInfo() {
        var userName = Prefs.getString(Constants.USER_NAME)
        if (userName.isEmpty()) {
            userName = getString(R.string.go_login)
            navHeaderBinding.tvUsername.text = userName
            return
        }
        AppUtil.isLogin = true
        val userId = Prefs.getString(Constants.USER_ID)
        navHeaderBinding.userIdLayout.visibility = View.VISIBLE
        navHeaderBinding.tvUserId.text = userId
        navHeaderBinding.tvUsername.text = userName
    }

    private fun switchFragment(position: Int) {
        val transaction = supportFragmentManager.beginTransaction()
        hideFragment(transaction)
        when(position) {
            Constants.FragmentIndex.HOME_INDEX ->
                homeFragment?.let {
                    transaction.show(it)
                } ?: kotlin.run {
                    ARouter.getInstance().build(RouterPath.Home.PAGE_HOME).navigation()
                        ?.let {
                            homeFragment = it as Fragment
                            homeFragment?.let {
                                binding.actionBar.tvTitle.text = getString(R.string.app_name)
                                transaction.add(R.id.container, it, null)
                            }
                        }
                }
            Constants.FragmentIndex.WECHAT_INDEX ->
                weChatFragment?.let { transaction.show(it) } ?: kotlin.run {
                    ARouter.getInstance().build(RouterPath.WeChat.PAGE_WECHAT).navigation()
                        ?.let {
                            weChatFragment = it as Fragment
                            weChatFragment?.let {
                                binding.actionBar.tvTitle.text = getString(R.string.wechat)
                                weChatFragment = it
                                transaction.add(R.id.container, it, null)
                            }
                        }
                }

            Constants.FragmentIndex.SYS_INDEX ->
                sysFragment?.let { transaction.show(it) } ?: kotlin.run {
                    ARouter.getInstance().build(RouterPath.Sys.PAGE_SYS).navigation()
                        ?.let {
                            sysFragment = it as Fragment
                            sysFragment?.let {
                                binding.actionBar.tvTitle.text = getString(R.string.knowledge_system)
                                sysFragment = it
                                transaction.add(R.id.container, it, null)
                            }
                        }
                }
            Constants.FragmentIndex.SQUARE_INDEX ->
                squareFragment?.let { transaction.show(it) } ?: kotlin.run {
                    ARouter.getInstance().build(RouterPath.Square.PAGE_SQUARE).navigation()
                        ?.let {
                            squareFragment = it as Fragment
                            squareFragment?.let {
                                binding.actionBar.tvTitle.text = getString(R.string.square)
                                squareFragment = it
                                transaction.add(R.id.container, it, null)
                            }
                        }
                }
            Constants.FragmentIndex.PROJECT_INDEX ->
                projectFragment?.let { transaction.show(it) } ?: kotlin.run {
                    ARouter.getInstance().build(RouterPath.Project.PAGE_PROJECT).navigation()
                        ?.let {
                            projectFragment = it as Fragment
                            projectFragment?.let {
                                binding.actionBar.tvTitle.text = getString(R.string.project)
                                projectFragment = it
                                transaction.add(R.id.container, it, null)
                            }
                        }
                }
        }
        mIndex = position
        transaction.commitAllowingStateLoss()
    }

    private fun hideFragment(transaction: FragmentTransaction) {
        homeFragment?.let { transaction.hide(it) }
        weChatFragment?.let { transaction.hide(it) }
        sysFragment?.let { transaction.hide(it) }
        squareFragment?.let { transaction.hide(it) }
        projectFragment?.let { transaction.hide(it) }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.nav_collect -> {
                ARouter.getInstance().build(RouterPath.Compose.PAGE_COMPOSE)
                    .navigation(this, object : NavigationCallback {
                        override fun onFound(postcard: Postcard) {
                        }

                        override fun onLost(postcard: Postcard) {
                        }

                        override fun onArrival(postcard: Postcard?) {
                        }

                        override fun onInterrupt(postcard: Postcard) {
                            val bundle = postcard.extras
                            ARouter.getInstance().build(RouterPath.LoginRegister.PAGE_LOGIN)
                                .with(bundle)
                                .withString(Constants.ROUTER_PATH, postcard.path)
                                .navigation()

                        }
                    })
            }
            R.id.nav_video -> {
                ARouter.getInstance().build(RouterPath.Media.PAGE_VIDEO).navigation()
            }
            R.id.nav_setting -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AppUtil.requestIgnoreBatteryOptimizations(this)
                }
            }
            R.id.nav_logout -> {
                if (AppUtil.isLogin) {
                    if (::loginOutService.isInitialized) { // Check if initialized
                        Log.d("MainActivity", "LoginOutService is initialized, proceeding with logout")
                        mainViewModel.logOut(loginOutService)
                    } else {
                        // Log an error to help diagnose the ARouter issue
                        Log.e("MainActivity", "LoginOutService is not initialized. Check ARouter configuration for service path: ${RouterPath.LoginRegister.SERVICE_LOGOUT}")
                        // Try to re-inject and check again
                        try {
                            ARouter.getInstance().inject(this)
                            if (::loginOutService.isInitialized) {
                                Log.d("MainActivity", "LoginOutService initialized after re-injection")
                                mainViewModel.logOut(loginOutService)
                            } else {
                                Log.e("MainActivity", "LoginOutService still not initialized after re-injection")
                                showToast("Logout service is currently unavailable. Please try again later.")
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error during ARouter injection: ${e.message}")
                            showToast("Logout service is currently unavailable. Please try again later.")
                        }
                    }
                } else {
                    ARouter.getInstance().build(RouterPath.LoginRegister.PAGE_LOGIN).navigation()
                    binding.drawerLayout.closeDrawers()
                }

            }
        }
        return false
    }

    override fun recreate() {
        kotlin.runCatching {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            homeFragment?.let {
                fragmentTransaction.remove(it)
            }
            squareFragment?.let {
                fragmentTransaction.remove(it)
            }
            sysFragment?.let {
                fragmentTransaction.remove(it)
            }
            projectFragment?.let {
                fragmentTransaction.remove(it)
            }
            weChatFragment?.let {
                fragmentTransaction.remove(it)
            }
            fragmentTransaction.commitAllowingStateLoss()
        }.onFailure {
            it.printStackTrace()
        }
        super.recreate()
    }

    /**
     * FAB 监听
     */
    private val onFABClickListener = View.OnClickListener {
//        when (mIndex) {
//            Constants.FragmentIndex.HOME_INDEX -> {
//                LiveDataBus.get().with(BusKey.SCROLL_TOP).value = ScrollEvent(0)
//            }
//            Constants.FragmentIndex.WECHAT_INDEX -> {
//                LiveDataBus.get().with(BusKey.SCROLL_TOP).value = ScrollEvent(1)
//            }
//            Constants.FragmentIndex.SYS_INDEX -> {
//                LiveDataBus.get().with(BusKey.SCROLL_TOP).value = ScrollEvent(2)
//            }
//            Constants.FragmentIndex.SQUARE_INDEX -> {
//                LiveDataBus.get().with(BusKey.SCROLL_TOP).value = ScrollEvent(3)
//            }
//            Constants.FragmentIndex.PROJECT_INDEX -> {
//                LiveDataBus.get().with(BusKey.SCROLL_TOP).value = ScrollEvent(4)
//            }
//        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currTabIndex", mIndex)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (System.currentTimeMillis().minus(mExitTime) <= 2000) {
                finish()
            } else {
                mExitTime = System.currentTimeMillis()
//                showToast("再按一次退出程序")
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

}