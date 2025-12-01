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
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ifprcrpgtcc.feirajovem.MainActivity
import com.ifprcrpgtcc.feirajovem.R
import java.util.Locale

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "default_channel"
    }

    /**
     * Recebe mensagem e exibe notificação (funciona também com app fechado quando FCM enviar mensagem 'notification')
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Mensagem recebida de: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Nova notificação"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Você recebeu uma atualização"

        sendNotification(title, body)
    }

    /**
     * Novo token — salvar em Firestore + Realtime DB e se possível (usuário logado) inscrever em tópico escola.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Novo token FCM: $token")

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Log.w(TAG, "Usuário não autenticado — token novo não salvo agora.")
            return
        }

        val firestore = FirebaseFirestore.getInstance()
        val rtdb = FirebaseDatabase.getInstance().getReference("tokens")

        // salva no Realtime
        rtdb.child(uid).setValue(token)
            .addOnSuccessListener { Log.d(TAG, "Token salvo no Realtime DB") }
            .addOnFailureListener { e -> Log.e(TAG, "Falha RTDB token: ${e.message}") }

        // salva no Firestore (merge)
        firestore.collection("usuarios").document(uid)
            .set(mapOf("token" to token), SetOptions.merge())
            .addOnSuccessListener { Log.d(TAG, "Token salvo no Firestore") }
            .addOnFailureListener { e -> Log.e(TAG, "Falha Firestore token: ${e.message}") }

        // tenta obter escola (Firestore -> RTDB fallback) e inscrever em tópico
        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val escola = doc.getString("escola")
                if (!escola.isNullOrBlank()) {
                    val topic = normalizeTopic(escola)
                    FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        .addOnSuccessListener { Log.d(TAG, "Inscrito no tópico: $topic") }
                        .addOnFailureListener { e -> Log.e(TAG, "Falha ao inscrever tópico: ${e.message}") }
                } else {
                    FirebaseDatabase.getInstance().getReference("usuarios").child(uid).get()
                        .addOnSuccessListener { snap ->
                            val escolaRt = snap.child("escola").getValue(String::class.java)
                            if (!escolaRt.isNullOrBlank()) {
                                val topic = normalizeTopic(escolaRt)
                                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                                    .addOnSuccessListener { Log.d(TAG, "Inscrito tópico RTDB: $topic") }
                                    .addOnFailureListener { e -> Log.e(TAG, "Falha subscrib RTDB: ${e.message}") }
                            } else {
                                Log.d(TAG, "Escola não encontrada (não será inscrito em tópico)")
                            }
                        }
                }
            }
            .addOnFailureListener { e -> Log.e(TAG, "Falha ao ler usuario Firestore: ${e.message}") }
    }

    private fun normalizeTopic(escola: String): String {
        return escola.lowercase(Locale.ROOT).replace("[^a-z0-9]".toRegex(), "_")
    }

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
