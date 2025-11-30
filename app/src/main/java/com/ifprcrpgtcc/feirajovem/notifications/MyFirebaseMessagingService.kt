package com.ifprcrpgtcc.feirajovem.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ifprcrpgtcc.feirajovem.MainActivity
import com.ifprcrpgtcc.feirajovem.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "default_channel"
    }

    /**
     * Chamado sempre que uma notificação push chega,
     * mesmo com o app fechado.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Mensagem recebida de: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: "Nova notificação"
        val body = remoteMessage.notification?.body ?: "Você recebeu uma atualização"

        sendNotification(title, body)
    }

    /**
     * Chamado sempre que o Firebase gera ou renova o token do dispositivo.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM: $token")
        salvarTokenNoFirestore(token)
    }

    /**
     * Salva o token FCM no Firestore em:
     * usuarios/{uid}/token
     */
    private fun salvarTokenNoFirestore(token: String) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            Log.w(TAG, "Usuário não autenticado — token não salvo agora.")
            return
        }

        val db = FirebaseFirestore.getInstance()

        val map = hashMapOf(
            "token" to token
        )

        db.collection("usuarios")
            .document(user.uid)
            .set(map, SetOptions.merge())
            .addOnSuccessListener {
                Log.d(TAG, "Token FCM salvo no Firestore.")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Erro ao salvar token no Firestore: ${e.message}")
            }
    }

    /**
     * Exibe a notificação no sistema (Android Notification Manager)
     */
    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(sound)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Criar canal no Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Notificações Gerais",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
