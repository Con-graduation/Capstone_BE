package dailyGuitar.capstone.service;

public interface EmailService {

    void send(String to, String subject, String htmlBody);
}


