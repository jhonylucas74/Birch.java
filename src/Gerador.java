import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.rmi.server.UID;
import java.util.ArrayList;


public class Gerador {
	
	private String nomeEntidade;
	private static ArrayList<Atributo> listaAtributos = new ArrayList<Atributo>();

	
	public static void main(String[] args) {
		
		String objeto = args[0];
		String entidade = objeto;
		listaAtributos = new ArrayList<Atributo>();
		
		boolean autoId = false; 
			
		
		int route= 1;
		boolean commando;
		
		System.out.println("=====================\nAnalisando parâmetros e criando as classes java: ");
		
		do {		
			commando = false;
			
			if (args[route].equals("-d")){
			    entidade = args[route+1];
			    route = route+2;
			    commando = true;
			    System.out.println(">> -d : Nomes diferentes para o Objeto java e a entidade relacional.");
			}
			
			if (args[route].equals("-id")){				
				autoId = true;
				listaAtributos.add(new Atributo("id","varchar(26)"));
				System.out.println(">> -id : Auto gerando o atributo id na classe java e no schema.");
			    route = route+1;
			    commando = true;
			}
		
		}while(commando);
		
		
		String temp,nome,tipo ="";
		
		for(int i = route; i<args.length ; i++ ){			
			temp= args[i];
			if (temp.contains(":")){
				nome = temp.substring(0,temp.indexOf(':'));
				tipo = temp.substring(temp.indexOf(':')+1,temp.length());
								
				listaAtributos.add(new Atributo(nome,tipo));
			}
		}
		
	
							
		createConnectoinFactory();		 				
		System.out.println("\n	ConnectionFactory.java");
		
		createDAO(objeto,entidade, listaAtributos);		
		System.out.println("	"+objeto+"DAO.java");
		
		createObject(objeto, listaAtributos,autoId);
		System.out.println("	"+objeto+".java");
		
		generateRake(entidade, listaAtributos);		
		System.out.println("	Tables.java\n")
		;
		System.out.println("Todos os arquivos foram criados com sucesso.");
				
		
	}
	
	
	/**
	 * Criando string da fábrica de conexão. 
	 */
	static private void createConnectoinFactory(){
		
		File f = new File(System.getProperty("java.class.path"));
		File dir = f.getAbsoluteFile().getParentFile();
		String pathDir = dir.toString();
		
		String path = pathDir+"/layouts/ConnectionFactoryLayout.txt";
		String layout =readFromFile(path);		
		
		if(layout == null){
			System.out.println("Erro ao localizar o arquivo ConnectionFactoryLayout.txt. Verifique a existência desse arquivo e de seu contéudo \n para uma correta geração do DAO da sua entidade.");
		}
		
		if(layout.length()==0){
			System.out.println("Erro ao carregar contéudo do arquivo ConnetionFactoryLayout.txt, nada foi encontrado.");
		}	
	
		
		saveFile(layout,"/ConnectionFactory.java");
	}

	
	static private void createDAO(String objeto,String tabela,ArrayList<Atributo> listaAtributos){
		
		File f = new File(System.getProperty("java.class.path"));
		File dir = f.getAbsoluteFile().getParentFile();
		String pathDir = dir.toString();
		
		String path = pathDir+"/layouts/DAOLayout.txt";
		String layout =readFromFile(path);		
		
		if(layout == null){
			System.out.println("Erro ao localizar o arquivo DAOLayout.txt. Verifique a existência desse arquivo e de seu contéudo \n para uma correta geração do DAO da sua entidade.");
		}
		
		if(layout.length()==0){
			System.out.println("Erro ao carregar contéudo do arquivo DAOLayout.txt, nada foi encontrado.");
		}	
		
		// SUBISTITUINDO @OBJETO
		layout = layout.replaceAll("@objeto", objeto);				
		
		// SUBISTITUINDO @TABELA
		layout = layout.replaceAll("@tabela", tabela);	

		// SUBISTITUINDO @ID
		layout = layout.replaceAll("@id", listaAtributos.get(0).getNome());	
		
		// SUBISTITUINDO @T_ID
		layout = layout.replaceAll("@t_id",convertAtributeType(listaAtributos.get(0).getTipo()));	
		
		// SUBISTITUINDO @M_ID
		layout = layout.replaceAll("@m_id",UpCase(0,convertAtributeName(listaAtributos.get(0).getNome())));
		
		// SUBISTITUINDO @LISTA_SET
		layout = layout.replaceAll("@lista_set",createListaSet(listaAtributos));
		
		// SUBISTITUINDO @LISTA_GET
		layout = layout.replaceAll("@lista_get",createListaGet(listaAtributos));
		
		// SUBISTITUINDO @ATRIBUTOS_SQL
		layout = layout.replaceAll("@atributos_sql",createAtributosSQL(listaAtributos));

		// SUBISTITUINDO @LISTA_UPDATE
		layout = layout.replaceAll("@lista_update",createListaUpdate(listaAtributos));
						
		saveFile(layout,"/"+objeto+"DAO.java");				
	}
	
	
	/* @lista_set
	 * Criando a lista de get, utilizadas no find() e all() 
	 * 
	 */
	static private String createListaSet(ArrayList<Atributo> listaAtributos){
		
		String listaSet="";
		
		for( Atributo a : listaAtributos){			
			listaSet += "obj.set"+UpCase(0,convertAtributeName(a.getNome()))+"(rs."+createGetType(a.getTipo())+"(\""+a.getNome()+"\"));" +"\n";
			listaSet +="                ";
		}				
		
		return listaSet;
	}
	
	/*
	 * Transformando o tipo sql como por exemplo varchar em getString()
	 */
	static private String createGetType(String tipo){
		tipo = convertAtributeType(tipo);
		
		if(tipo.equals("String")){
			return "getString";
		}
		
		if(tipo.equals("int")){
			return "getInt";
		}
		
		if(tipo.equals("Double")){
			return "getDouble";
		}
		
		if(tipo.equals("boolean") || tipo.equals("Boolean") ){
			return "getBoolean";
		}
		
		if(tipo.equals("Date")){
			return "getDate";
		}
		
		return "";
	}
	
	/*
	 * Criando a lista_get utilizada no create()
	 */
	static private String createListaGet(ArrayList<Atributo> listaAtributos){
		
		String listaSet="";
		
		for( int i=0;i<listaAtributos.size();i++){		
			Atributo a = listaAtributos.get(i);
			
			if(i!= listaAtributos.size()-1){
				listaSet +="sql += obj.get"+UpCase(0,convertAtributeName(a.getNome()))+"()+\"','\";"+"\n";
				listaSet +="            ";
			}else{
				listaSet +="sql += obj.get"+UpCase(0,convertAtributeName(a.getNome()))+"()+\"');\";"+"\n";
			}
		}				
		
		return listaSet;
	}
	
	
	
	static private String createAtributosSQL(ArrayList<Atributo> listaAtributos){
		
		String listaSet="";
		
		for( int i=0;i<listaAtributos.size();i++){		
			Atributo a = listaAtributos.get(i);
			
			if(i!= listaAtributos.size()-1){
				listaSet += a.getNome()+", ";
				
			}else{
				listaSet += a.getNome();
			}
		}				
		
		return listaSet;
	}
	
	
	static private String createListaUpdate(ArrayList<Atributo> listaAtributos){
		
		String listaSet="";
		
		for( int i=0;i<listaAtributos.size();i++){		
			Atributo a = listaAtributos.get(i);
			
			if(i == 0){				
				listaSet +="\""+a.getNome()+" = '\"+ obj.get"+UpCase(0,convertAtributeName(a.getNome())) +"()"+"\n        "+"+\"', ";				
			}
			else if(i!= listaAtributos.size()-1){
				listaSet +=a.getNome()+" = '\"+ obj.get"+UpCase(0,convertAtributeName(a.getNome())) +"()"+"\n        "+"+\"', ";
				
			}else{
				listaSet +=a.getNome()+" = '\"+ obj.get"+UpCase(0,convertAtributeName(a.getNome())) +"()+\"' \"";
				listaSet += "+\" where "+listaAtributos.get(0).getNome()+" = '\"+id+\"' ;\"";
			}
		}				
		
		return listaSet;
	}
	
		
	
	static private String convertAtributeName(String att){		
		
		while(att.contains("_")){
			int target =  att.indexOf("_");
		    att =   att.replaceFirst("_", "") ;		    		    		  
		    att = UpCase(target,att); 		    
		}
		
		return att;
	}
	
	static private String UpCase(int target,String att){		
	    char sub = att.charAt(target);
	    String nsub = String.valueOf(sub); 
	    nsub = nsub.toUpperCase();
	    
	    String out="";
	    for(int i= 0; i<att.length();i++){
	    	if(i!=target){
	    		out+=att.charAt(i);
	    	}else{
	    		out+=nsub;
	    	}
	    }		
	    return out;
	}
	
	
static private void createObject(String objeto,ArrayList<Atributo> listaAtributos,boolean autoId){
	
	File f = new File(System.getProperty("java.class.path"));
	File dir = f.getAbsoluteFile().getParentFile();
	String pathDir = dir.toString();
	
	String path = pathDir+"/layouts/ObjectLayout.txt";
	String layout =readFromFile(path);		
	
	if(layout == null){
		System.out.println("Erro ao localizar o arquivo DAOLayout.txt. Verifique a existência desse arquivo e de seu contéudo \n para uma correta geração do DAO da sua entidade.");
	}
	
	if(layout.length()==0){
		System.out.println("Erro ao carregar contéudo do arquivo DAOLayout.txt, nada foi encontrado.");
	}	
	
	
	
	// SUBISTITUINDO @AUTO_ID
	layout = layout.replaceAll("@auto_id", createAutoID(autoId));
	
	// SUBISTITUINDO @OBJETO
	layout = layout.replaceAll("@objeto", objeto);	
	
	// SUBISTITUINDO @OBJETO
    layout = layout.replaceAll("@atributos_objeto", createAtributosObjeto(listaAtributos));
    
    // SUBISTITUINDO @GETTERS&SETTERS
    layout = layout.replaceAll("@getters&setters", createGettersESetters(listaAtributos));	
    
    if(autoId){
    	String imports = "import java.rmi.server.UID;\n\n"+layout;
    	layout = imports;
    }
	
    saveFile(layout,"/"+objeto+".java");
}

static private String createAutoID(boolean autoId){
	
	if(autoId){
		return "public @objeto(){ \n"+
	    "        UID id = new UID();\n"+    	    
	    "        this.id= String.valueOf(id);\n"+
	    "    }\n";
	}
	
	return "";
}

static private String createGettersESetters(ArrayList<Atributo> listaAtributos){
	
	String listaSet="";
	
	for( int i=0; i<listaAtributos.size(); i++){		
		Atributo a = listaAtributos.get(i);
		
    	listaSet += "public "+convertAtributeType(a.getTipo())+" get"+UpCase(0,convertAtributeName(a.getNome()))+"(){ "+
    	"\n        return "+convertAtributeName(a.getNome())+";\n"
    	+"    }\n\n";
    	
    	listaSet += "     public void set"+UpCase(0,convertAtributeName(a.getNome()))+"("+convertAtributeType(a.getTipo())+" "+convertAtributeName(a.getNome())+"){ "+
    	"\n        this."+convertAtributeName(a.getNome())+" = "+convertAtributeName(a.getNome())+";"
    	+"\n    }\n\n    ";		
		
	}				 
	
	return listaSet;
}


static private String createAtributosObjeto(ArrayList<Atributo> listaAtributos){
	
	String listaSet="";
	
	for( int i=0;i<listaAtributos.size();i++){		
		Atributo a = listaAtributos.get(i);
		
		listaSet += "private "+convertAtributeType(a.getTipo())+" "+convertAtributeName(a.getNome())+";\n    ";
	}				
	
	return listaSet;
}

static private String convertAtributeType(String tipo){
	
	if(tipo.contains("varchar")){	
		return "String";
	}
	
	if(tipo.contains("char")){	
		return "String";
	}
	
	if(tipo.contains("smallint") || tipo.contains("int")){	
		return "int"; 
	}
	
	if(tipo.contains("float") || tipo.contains("real") ){	
		return "Double"; 
	}
	
	
	
	return tipo;
}

	
static private String readFromFile(String path) {
        
        String ret = "";
         
        try {
        	File arquivo = new File(path) ; 
            FileInputStream inputStream = new FileInputStream(arquivo);
                                     
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                 
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString+"\n");
                }
                 
                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
        	return null;
           
        } catch (IOException e) {
        	
        }
 
        return ret;
    }

   	static private void generateRake(String tabela,ArrayList<Atributo> listaAtributos){

   		File fd = new File(System.getProperty("java.class.path"));
   		File dir = fd.getAbsoluteFile().getParentFile();
   		String pathDir = dir.toString();
   		
   		File f = new File(System.getProperty("user.dir")+"/Tables.java");
   		String path;
   		
   		if(f.exists()){
   			path = System.getProperty("user.dir")+"/Tables.java";   			
   		}else {
   			path = pathDir+"/layouts/RakeLayout.txt";
   		}
   		
   		String layout =readFromFile(path);		
   		
   		if(layout == null){
   			System.out.println("Erro ao localizar o arquivo RakeLayout.txt. Verifique a existência desse arquivo e de seu contéudo \n para uma correta geração do DAO da sua entidade.");
   		}
   		
   		if(layout.length()==0){
   			System.out.println("Erro ao carregar contéudo do arquivo RakeLayout.txt, nada foi encontrado.");
   		}	
   		
   		// SUBISTITUINDO @OBJETO
   		layout = layout.replaceAll("//@SQLS", createTable(tabela, listaAtributos));
   		
   		saveFile(layout,"/Tables.java");
   	}
   	
   	static private String createTable(String tabela, ArrayList<Atributo> listaAtributos){
   		
   		String listaSet="//@SQLS \n\n";
   		listaSet+="        sqls.add(\"CREATE TABLE IF NOT EXISTS "+tabela+" ( \"\n";
   		
   		for( int i=0;i<listaAtributos.size();i++){		
   			Atributo a = listaAtributos.get(i);   		
   			listaSet +="        +\""+a.getNome()+" "+a.getTipo()+",\" \n";     			
   		}				
   		listaSet+="        +\"PRIMARY KEY("+listaAtributos.get(0).getNome()+") );\");";  
   		
   		return listaSet;
   	}
   	
   	static private void  saveFile(String data,String file){
   	     				
   		File myFile = new File(System.getProperty("user.dir")+file);
        try {
			 myFile.createNewFile();
			 FileOutputStream fOut = new FileOutputStream(myFile);
             OutputStreamWriter myOutWriter =  new OutputStreamWriter(fOut);
             myOutWriter.append(data);
             myOutWriter.close();
             fOut.close();
		} catch (IOException e) {			
			e.printStackTrace();
		}
   	}
 
}
