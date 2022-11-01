import java.security.Security;
import java.util.ArrayList;
//import java.util.Base64;
import java.util.HashMap;
//import com.google.gson.GsonBuilder;
import java.util.Scanner;

public class VQT_Blockchain {

    public static ArrayList<VNPT_Tu> blockchain = new ArrayList<VNPT_Tu>();
    public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();

    public static int difficulty = 4;
    public static float minimumTransaction = 0.1f;
    public static Mobie mobie1; //Kho điện thoại 1
    public static Mobie mobie2; //Kho điện thoại 2
    public static Transaction genesisTransaction;

    public static void main(String[] args) {
        int i=0;
        // Nhập tu ban phim
        Scanner BlockData = new Scanner(System.in);
        //add our blocks to the blockchain ArrayList:
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Thiết lập bảo mật bằng phương thức BouncyCastleProvider

        //Create wallets:
        mobie1 = new Mobie();
        mobie2 = new Mobie();
        Mobie coinbase = new Mobie();

        //Khởi tạo số lượng điện thoại trong kho 1

        System.out.println("Nhập số lượng điện thoại trong kho 1: ");
        int x = Integer.parseInt(BlockData.nextLine());
        genesisTransaction = new Transaction(coinbase.publicKey, mobie1.publicKey, x, null);
        genesisTransaction.generateSignature(coinbase.privateKey);//Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        System.out.println("Nhập số lượng điện thoại trong kho 2: ");
        int y = Integer.parseInt(BlockData.nextLine());
        genesisTransaction = new Transaction(coinbase.publicKey, mobie2.publicKey, y, null);
        genesisTransaction.generateSignature(coinbase.privateKey);//Gán private key (ký thủ công) vào giao dịch gốc
        genesisTransaction.transactionId = "0"; //Gán ID cho giao dịch gốc
        genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //Thêm Transactions Output
        UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //Lưu giao dịch đầu tiên vào danh sách UTXOs.

        System.out.println("Đang tạo và đào khối gốc .... ");
        VNPT_Tu genesis = new VNPT_Tu("0");
        genesis.addTransaction(genesisTransaction);
        addBlock(genesis);



        //Thử nghiệm
        VNPT_Tu block1 = new VNPT_Tu(genesis.hash);
        System.out.println("\nSố dư điện thoại trong kho 1 là : " + mobie1.getBalance());
        System.out.println("\nSố dư điện thoại trong kho 2 là : " + mobie2.getBalance());
        System.out.println("\nGiao dịch số lượng điện thoại từ kho 1 đến kho 2 là: ");
        int z = Integer.parseInt(BlockData.nextLine());
        block1.addTransaction(mobie1.sendFunds(mobie2.publicKey, z));
        ///System.out.println("Hãy nhập số lượng điện thoại cần chuyển: ");
        addBlock(block1);

        System.out.println("\nSố dư mới điện thoại trong kho 1 là : " + mobie1.getBalance());
        System.out.println("Số dư điện thoại trong kho 2 là : " + mobie2.getBalance());

        isChainValid();

    }

    public static Boolean isChainValid() {
        VNPT_Tu currentBlock;
        VNPT_Tu previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //Tạo một danh sách hoạt động tạm thời của các giao dịch chưa được thực thi tại một trạng thái khối nhất định.
        tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));

        //loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {

            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            //Kiểm tra, so sánh mã băm đã đăng ký với mã băm được tính toán
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Mã băm khối hiện tại không khớp");
                return false;
            }
            //So sánh mã băm của khối trước với mã băm của khối trước đã được đăng ký
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Mã băm khối trước không khớp");
                return false;
            }
            //Kiểm tra xem mã băm có lỗi không
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#Khối này không đào được do lỗi!");
                return false;
            }

            //Vòng lặp kiểm tra các giao dịch
            TransactionOutput tempOutput;
            for(int t = 0; t < currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);

                if(!currentTransaction.verifySignature()) {
                    System.out.println("#Chữ ký số của giao dịch (" + t + ") không hợp lệ");
                    return false;
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Các đầu vào không khớp với đầu ra trong giao dịch (" + t + ")");
                    return false;
                }

                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);

                    if(tempOutput == null) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") bị thiếu!");
                        return false;
                    }

                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Các đầu vào tham chiếu trong giao dịch (" + t + ") có giá trị không hợp lệ");
                        return false;
                    }

                    tempUTXOs.remove(input.transactionOutputId);
                }

                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }

                if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
                    System.out.println("#Giao dịch(" + t + ") có người nhận không đúng!");
                    return false;
                }
                if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
                    System.out.println("#Đầu ra của giao (" + t + ") không đúng với người gửi.");
                    return false;
                }

            }

        }
        System.out.println("Chuỗi khối hợp lệ!");
        return true;
    }

    public static void addBlock(VNPT_Tu newVNPTTu) {
        newVNPTTu.mineBlock(difficulty);
        blockchain.add(newVNPTTu);
    }
}

