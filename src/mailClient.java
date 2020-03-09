import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.*;
import java.io.*;
import java.util.Base64;


/**
 * Open an SMTP connection to a mailserver and send one mail.
 *
 */
public class mailClient {

    /* The socket to the server */
    private Socket connection;
    // Picture objects that needed to load picture and encode it to Base64 string
    BufferedImage bufferimage;
    ByteArrayOutputStream output;
    byte[] bytes;
    String base64;


    /* Streams for reading and writing the socket */
    private BufferedReader fromServer;
    private DataOutputStream toServer;
    private static final int SMTP_PORT = 25;

    //This simulates next line.
    private static final String CRLF = "\r\n";

    /* Are we connected? Used in close() to determine what to do. */
    private boolean isConnected = false;

    /* Create an SMTPConnection object. Create the socket and the
       associated streams. */
    public mailClient() throws IOException {
        // Founded on the internet
        // Converts picture to Base64 string
        bufferimage = ImageIO.read(new File("/zhome/44/b/130826/IdeaProjects/MailClient/src/s180103.png"));
        output = new ByteArrayOutputStream();
        ImageIO.write(bufferimage, "png", output );
        bytes = output.toByteArray();
        base64 = Base64.getEncoder().encodeToString(bytes);


        // Initialize SMTP connection.
        connection = new Socket("localhost", SMTP_PORT);
        // Initialize streams to socket for read from server and write output to server
        fromServer = new BufferedReader((new InputStreamReader(connection.getInputStream())));
        toServer =   new DataOutputStream(connection.getOutputStream());


	/* Read a line from server and check that the reply code is 220.
	   If not, throw an IOException. */
        if(!(parseReply(fromServer.readLine())==220)){
            throw new IOException();
        }


	/* SMTP handshake. We need the name of the local machine.
	   Send the appropriate SMTP handshake command. */
        String localhost ="localhost";
        sendCommand("HELO "+localhost,250);

        isConnected = true;
    }

    /* Send the message. Write the correct SMTP-commands in the
       correct order. No checking for errors, just throw them to the
       caller. */
    public void send() throws IOException {

        // Mail from information.
        sendCommand("MAIL FROM: s171242@student.dtu.dk",250);

	/* Send all the necessary commands to send a message. Call
	   sendCommand() to do the dirty work. Do _not_ catch the
	   exception thrown from sendCommand(). */
        // took inspiration from https://community.spiceworks.com/how_to/36534-test-email-flow-by-sending-an-email-with-an-attachment-using-smtp-command
        // took also inspiraton from Bhupjit Singh
        // After a lot of research on internet I was only able to send pictures with these line

        // Mail to information.
        sendCommand("RCPT TO: s180103@student.dtu.dk",250);

        // Data command is needed before inserting data to server.
        sendCommand("DATA",354);
        // Subject header og email
        sendCommand("Subject:s180103",0);

        //These lines makes sure that picture is sent as attachment to mail.
        sendCommand("MIME-Version: 1.0",0);


        sendCommand("Content-Type:multipart/mixed;boundary=seperator",0);
        sendCommand("--seperator",0);
        //Picture filename stands here.
        sendCommand("Content-Type:application/octet-stream;name=\"s180103.png\"",0);
        sendCommand("Content-Transfer-Encoding:base64",0);
        sendCommand("Content-Disposition:attachment;filename=\"s180103.png\"",0);
        //These empty lines are needed to send the picture.
        sendCommand("",0);
        // Base64 string (The picture)
        sendCommand(base64,0);

        sendCommand("",0);
        sendCommand("",0);
        sendCommand("--seperator",0);
        sendCommand(" ",0);
        sendCommand(" ",0);
        // Text message
        sendCommand("s180103@student.dtu.dk",0);
        sendCommand("Mandatory assignment",0);
        sendCommand("",0);
        sendCommand(".",250);


    }

    /* Close the connection. First, terminate on SMTP level, then
       close the socket. */
    public void close() {
        isConnected = false;

        try {
            sendCommand("QUIT",221);
            connection.close();
            fromServer.close();
            toServer.close();
        } catch (IOException e) {
            System.out.println("Unable to close connection: " + e);
            isConnected = true;
        }
    }

    /* Send an SMTP command to the server. Check that the reply code is
       what is is supposed to be according to RFC 821. */
    private void sendCommand(String command, int rc) throws IOException {

        /* Write command to server and read reply from server. */
        //CLRF is needed for every message/command after it is written in the method to execute.
        toServer.writeBytes(command+CRLF);

        if (rc==0)
            return;

	/* Check that the server's reply code is the same as the parameter
	   rc. If not, throw an IOException. */

        if(!(parseReply(fromServer.readLine())==rc)){
            throw new IOException();
        }
    }

    /* Parse the reply line from the server. Returns the reply code. */
    private int parseReply(String reply) {
        //We need to read first 3 character of the message from server
        String temp = reply.substring(0,3);
        int tempint = Integer.parseInt(temp);
        return tempint;
    }

    /* Destructor. Closes the connection if something bad happens. */

    protected void finalize() throws Throwable {
        if(isConnected) {
            close();

        }
        super.finalize();
    }

    public static void main(String[] args) {
        try {
            mailClient test = new mailClient();
            test.send();
            test.close();
        }catch (IOException e){
            System.out.println("something is wrong");
        }
    }
}
