package com.example.notificationforwarder.service.email

import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.Properties

class EmailService {
    private lateinit var session: Session
    private var properties = Properties()

    fun configure(host: String, port: Int, username: String, password: String) {
        properties.apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", port)
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
        }

        session = Session.getInstance(properties, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })
    }

    suspend fun sendEmail(to: String, subject: String, content: String) {
        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(session.properties["username"] as String))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
                setSubject(subject)
                setText(content)
            }
            Transport.send(message)
        } catch (e: MessagingException) {
            throw EmailException("Erreur lors de l'envoi de l'email", e)
        }
    }
}

class EmailException(message: String, cause: Throwable? = null) : Exception(message, cause) 