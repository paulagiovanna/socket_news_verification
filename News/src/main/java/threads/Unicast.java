package threads;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import java.security.Signature;
import java.security.SignatureException;

import org.json.JSONException;
import org.json.JSONObject;

import objects.PublicObjects;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;



public class Unicast extends Thread implements PublicObjects{
	
	protected DatagramSocket ds_unicast;
	KeyPairGenerator key_gen;
	KeyPair key_pair;
	PublicKey public_key;
	PrivateKey private_key;
	
	public Unicast() throws NoSuchAlgorithmException {
		try {
			ds_unicast = new DatagramSocket();
		
			this.key_gen = KeyPairGenerator.getInstance("RSA");
			SecureRandom secRan = new SecureRandom();
			this.key_gen.initialize(2048, secRan);
			this.key_pair = key_gen.generateKeyPair();
			this.public_key = key_pair.getPublic();
			this.private_key = key_pair.getPrivate();
			
		}
		catch(Exception e){
			System.out.println("Error in Unicast method:");
			e.printStackTrace();
		}
		
	}
	
	public void send_public_key_to_multicast_group() {
		String message = Base64.getMimeEncoder().encodeToString(public_key.getEncoded());
		try {
			DatagramPacket dp_multicast = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName("224.0.0.1"), 8080);
			this.ds_unicast.send(dp_multicast);
			
		}
		catch(Exception e){
			System.out.println("Error in send_to_multicast method:");
			e.printStackTrace();
		}
	}
	
	public void send_sign_to_multicast(String message) throws InvalidKeyException, SignatureException, IOException, JSONException {
        Signature sig;
		try {
			sig = Signature.getInstance("MD5withRSA");
			sig.initSign(this.private_key);
	        sig.update(message.getBytes());
	        byte[] assinatura_byte = sig.sign();
	        
	        JSONObject json_message = new JSONObject();
	        json_message.put("Message", message);
	        json_message.put("Signature", assinatura_byte);
	        
	        System.out.println("original: " + json_message);
	        
	        byte[] final_message_byte = json_message.toString().getBytes();
	        
	        /******************************************************************************************************/

	        JSONObject new_json_message = new JSONObject(new String(final_message_byte));
	        
	        /*
	        sig.initVerify(this.public_key);
		    sig.update(new String(new_json_message.getString("Message")).getBytes());
		    
		    if (sig.verify(a)) {
		        //Mensagem corretamente assinada
		        System.out.println("A Mensagem recebida foi assinada corretamente.");
		     } else {
		         //Mensagem não pode ser validada
        		System.out.println("A Mensagem recebida NÃO pode ser validada.");
		    }*/
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Error in sign_message method:");
			e.printStackTrace();
		}
	}
	
	public void send_to_multicast(byte[] message){		
		try {		
			DatagramPacket dp_multicast = new DatagramPacket(message, message.length, InetAddress.getByName("224.0.0.1"), 8080);
			this.ds_unicast.send(dp_multicast);
		}
		catch(Exception e){
			System.out.println("Error in send_to_multicast method:");
			e.printStackTrace();
		}
	}
	
	public void send_to_unicast(String message, InetAddress unicast_ip, int unicast_port){
		try {
			//Cria a variavel responsavel por enviar mensagens, é tipo um túnel pelo qual vai passar a informacao. Tipo o carteiro: So transporta a carta.			
			DatagramPacket dp_unicast = new DatagramPacket(message.getBytes(), message.length(), unicast_ip, unicast_port);
			//Pega o carteiro para enviar a carta.
			this.ds_unicast.send(dp_unicast);

			}
			catch(Exception e){
				System.out.println("Error in send_to_unicast method:");
				e.printStackTrace();
			}
		}
	
	@SuppressWarnings({ "static-access", "rawtypes" })
	public void run() {
		try {			
			//deixa a porta sempre aberta para receber mensagens do grupo.
			while(true) {
				//cria a variavel que posteriormente vai receber a mensagem.
				//Por ser um datagrampacket de recebimento voce nao preenche ao criar. Ele vai ser preenchido posteriormente com as informações que chegaram.
				DatagramPacket dp = new DatagramPacket(new byte[1024], 1024);
				
				//se algo for recebido na porta 'receive_port' ele aloca o pacote na variavel anteriormente criada.
				
				this.ds_unicast.receive(dp);
				
				//Printa os dados do pacote em forma de string.
				String data = new String(dp.getData());
				System.out.println("\n" + dp.getPort() + " says by unicast:\n" + data);
				
				if (!this.public_key_map.containsKey(dp.getPort())){
					PublicKey publick  = load_public_key(data);
					this.public_key_map.put(dp.getPort(), publick);
					this.news.put(new SimpleEntry(dp.getPort(), "Total"), 0);
					this.news.put(new SimpleEntry(dp.getPort(), "Fake"), 0);
				}
			}
		}
		//Caso ocorra algum erro ele vai ser pego pelo catch.
		catch(Exception e){
			System.out.println("Error in run method:");
			//Printa o erro.
			e.printStackTrace();
		}

	}
	public static PublicKey load_public_key(String datastring) throws GeneralSecurityException, IOException{
		byte[] data = Base64.getMimeDecoder().decode((datastring.getBytes()));
		X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
		KeyFactory fact = KeyFactory.getInstance("RSA");
		return fact.generatePublic(spec);
	}
}
