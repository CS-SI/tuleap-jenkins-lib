#!/usr/bin/groovy
import groovy.json.JsonSlurper;
import groovy.json.JsonBuilder;
import java.net.URLEncoder;

/* Exemple d'utilisation (en cas de succès) :
 * 
 *  withCredentials([
 *        string(credentialsId: 'GIT_CRED_ID', variable: 'token_git'),
 *  ]){
 *    sendTuleapStatus gitToken: this.env.token_git,
 *                     targetRepo: 'projet/depot-test.git',
 *                     status: "success"
 *  }
 */ 

def call(Map config) {

  // Configuration de la step :
  //   gitToken : Token d'accès au dépot GIT
  //   tuleapServer : chemin vers le server Tuleap
  //   targetRepo : ID ou chemin du dépot
  //   status : success / failure
  def gitToken = config.gitToken
  def serverPath = config.tuleapServer
  def targetRepoId = config.targetRepo ? URLEncoder.encode(config.targetRepo, "UTF-8") : 0
  def status = config.status
  if (config.status == null) {
    // If status not explicitly set, use current build's status
    if(currentBuild.resultIsBetterOrEqualTo("SUCCESS")) {
      status = "success"
    } else {
      status = "failure"
    }
  }
  // Ensure status is lower case (for example, currentBuild is upper case)
  status = status.toLowerCase()

  if (serverPath == null) {
    error "The tuleapServer parameter must be set"
  }

  // récupération de l'ID du dernier commit
  def version = env.GIT_COMMIT

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
