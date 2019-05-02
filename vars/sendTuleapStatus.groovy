#!/usr/bin/groovy
import groovy.json.JsonSlurper;
import groovy.json.JsonBuilder;
import java.net.URLEncoder;

/* Exemple d'utilisation (en cas de succès) :
 * 
 *  withCredentials([
 *        string(credentialsId: 'GIT_CRED_ID', variable: 'token_git'),
 *        string(credentialsId: 'TULEAP_API_ID', variable: 'token_tuleap')
 *  ]){
 *    sendTuleapStatus gitToken: this.env.token_git,
 *                     apiRestToken: this.env.token_tuleap,
 *                     targetRepo: 'projet/depot-test.git',
 *                     status: "success"
 *  }
 */ 

def call(Map config) {

  // Configuration de la step :
  //   gitToken : Token d'accès au dépot GIT
  //   apiRestToken = token d'accès à l'API REST Tuleap
  //   tuleapServer = chemin vers le server Tuleap (tuleap.net par defaut)
  //   targetRepo = ID ou chemin du dépot
  //   status = success / failure
  def gitToken = config.gitToken
  def apiRestToken = config.apiRestToken
  def serverPath = config.tuleapServer ?: "https://tuleap.net"
  def targetRepoId = config.targetRepo ? URLEncoder.encode(config.targetRepo, "UTF-8") : 0
  def status = (config.status == "success") ? "success" : "failure"

  // récupération de l'ID du dernier commit
  def version = sh( script: 'git rev-parse HEAD', returnStdout: true).toString().trim()

  retry(3){
    // Connexion vers l'API Tuleap
    def http = new URL("${serverPath}/api/git/${targetRepoId}/statuses/${version}").openConnection() as HttpURLConnection
    
    // Contenu du message
    def json  = new groovy.json.JsonBuilder()
    json state: status,
         token: gitToken
    def message = json.toString()

    // Envoi de la requete
    http.setRequestMethod('POST')
    http.setDoOutput(true)
    http.setRequestProperty("Accept", 'application/json')
    http.setRequestProperty("Content-Type", 'application/json; charset=UTF-8')
    http.setRequestProperty("X-Auth-AccessKey", apiRestToken)
    http.outputStream.write(message.getBytes("UTF-8"))
    
    int retcode = 1;
    try {
      http.connect()
    } finally {
      retcode = http.responseCode
      http.disconnect()
    }

    echo "Envoi de '${message}' à l'URL ${serverPath}/api/git/${targetRepoId}/statuses/${version}  (retcode = ${retcode})"
    
    if (retcode != 201) {
      // Si le code de retour n'est pas 201, on met le build UNSTABLE
      echo "Pipeline status : Unstable car l'API Tuleap a renvoyé le code d'erreur ${retcode} lors de la mise à jour du status"
      currentBuild.result = 'UNSTABLE'
    }
      
    return retcode
  }
}
