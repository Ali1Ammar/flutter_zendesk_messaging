import android.content.Intent
import android.util.Log
import com.chyiiiiiiiiiiiiii.zendesk_messaging.ZendeskMessagingPlugin
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import zendesk.android.Zendesk
import zendesk.android.ZendeskResult
import zendesk.android.ZendeskUser
import zendesk.messaging.android.DefaultMessagingFactory
import io.flutter.plugin.common.MethodChannel.Result

class ZendeskMessaging(private val plugin: ZendeskMessagingPlugin, private val channel: MethodChannel,private val result: Result) {
    companion object {
        const val tag = "[ZendeskMessaging]"

        // Method channel callback keys
        const val initializeSuccess: String = "initialize_success"
        const val initializeFailure: String = "initialize_failure"
        const val loginSuccess: String = "login_success"
        const val loginFailure: String = "login_failure"
        const val logoutSuccess: String = "logout_success"
        const val logoutFailure: String = "logout_failure"
    }

    fun initialize(channelKey: String) {
        Log.i(tag,"Channel Key - $channelKey")
        Zendesk.initialize(
            plugin.activity!!,
            channelKey,
            successCallback = { value ->
                plugin.isInitialize = true;
                result.success("initialize success - $value")
                channel.invokeMethod(initializeSuccess, null)
            },
            failureCallback = { error ->
                plugin.isInitialize = false;
                 Log.i(tag,"initialize failure - $error")
                error.message
                result.error("error","initialize failure - ${error.message}",null)
                channel.invokeMethod(initializeFailure, mapOf("error" to error.message))
            },
            messagingFactory = DefaultMessagingFactory()
        )
    }

    fun show() {
        Zendesk.instance.messaging.showMessaging(plugin.activity!!, Intent.FLAG_ACTIVITY_NEW_TASK)
        result.success("show")
         Log.i(tag,"show")
    }

    fun loginUser(jwt: String) {
        Zendesk.instance.loginUser(
            jwt,
            { value: ZendeskUser? ->
                value?.let {
                    channel.invokeMethod(loginSuccess, mapOf("id" to it.id, "externalId" to it.externalId))
                } ?: run {
                    channel.invokeMethod(loginSuccess, mapOf("id" to null, "externalId" to null))
                }
            },
            { error: Throwable? ->
                 Log.i(tag,"Login failure : ${error?.message}")
                 Log.i(tag,error.toString())
                channel.invokeMethod(loginFailure, mapOf("error" to error?.message))
            })
    }

    fun logoutUser() {
        GlobalScope.launch (Dispatchers.Main)  {
            try {
                val result = Zendesk.instance.logoutUser()
                if (result is ZendeskResult.Failure) {
                    channel.invokeMethod(logoutFailure, null)
                } else {
                    channel.invokeMethod(logoutSuccess, null)
                }
            } catch (error: Throwable) {
                 Log.i(tag,"Logout failure : ${error.message}")
                channel.invokeMethod(logoutFailure, mapOf("error" to error.message))
            }
        }
    }
}
