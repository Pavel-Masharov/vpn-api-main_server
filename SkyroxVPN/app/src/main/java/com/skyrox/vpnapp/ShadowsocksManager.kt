import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import com.github.shadowsocks.bg.TransproxyService

class ShadowsocksManager(private val context: Context) {

//    fun startShadowsocks() {
//        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
//        with(sharedPref.edit()) {
//            putString("proxy_host", "your.shadowsocks.server")
//            putInt("proxy_port", 8388)
//            putString("proxy_password", "your_password")
//            putString("proxy_encrypt_method", "chacha20-ietf-poly1305")
//            apply()
//        }
//
//        val intent = Intent(context, TransproxyService::class.java)
//        context.startService(intent)
//    }
//
//    fun stopShadowsocks() {
//        context.stopService(Intent(context, TransproxyService::class.java))
//    }
}
