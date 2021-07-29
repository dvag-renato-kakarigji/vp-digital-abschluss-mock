function fn() {
  var env = karate.env; // get java system property 'karate.env'
  var config = {};
  if (!env) {
    env = 'default'; // a custom 'intelligent' default
  }

  if( env == 'default' ){
    config.host = 'http://localhost:8080';
    config.externalHost = 'http://localhost:8080';
    config.zobHost = 'https://zob.vdl.k8s.dvag.net';
    config.zobUser = '1000000';
    config.zobPassword = '1000000';
  } else if( env == 'entwicklung' ){
    config.host = 'https://springbootonkubernetes.entwicklung.k8s.dvag.net';
    config.externalHost = 'https://springbootonkubernetes.entwicklung.dvag';
    config.zobHost = 'https://hsz.entw-mydvag.com';
    config.zobUser = karate.properties['user'];
    config.zobPassword = karate.properties['password'];
    karate.configure('proxy', 'http://proxy.infra.dvag.net:3128');
  } else if( env == 'integration' ){
    config.host = 'https://springbootonkubernetes.integration.k8s.dvag.net';
    config.externalHost = 'https://springbootonkubernetes.integration.dvag';
    config.zobHost = 'https://hsz.intg-mydvag.com';
    config.zobUser = karate.properties['user'];
    config.zobPassword = karate.properties['password'];
    karate.configure('proxy', 'http://proxy.infra.dvag.net:3128');
  } else if( env == 'abnahme' ){
    config.host = 'https://springbootonkubernetes.abnahme.k8s.dvag.net';
    config.externalHost = 'https://springbootonkubernetes.abnahme.dvag';
    config.zobHost = 'https://hsz.test-mydvag.com';
    config.zobUser = karate.properties['user'];
    config.zobPassword = karate.properties['password'];
    karate.configure('proxy', 'http://proxy.infra.dvag.net:3128');
  }

  var LM = Java.type('com.dvag.devops.springbootonkubernetes.CredentialsLogModifier');
  karate.configure('logModifier', LM.INSTANCE);

  return config;
}
