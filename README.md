# Spring Boot auf Kubernetes - ein Beispiel

## Inhalt

Dieses Repository ist eine Beispielanwendung für den Betrieb einer Spring Boot Anwendung auf dem Container-Orchestrierer
Kubernetes. Die Anwendung dient als Beispiel und ausdrücklich **nicht** als Vorgabe für die Entwicklung eines Services mit
Spring Boot und Kubernetes. Vielmehr ist sie eine Sammlung von Best Practices die Entwicklern helfen soll ohne viel Recherche
dem Betriebskonzept genügende Anwendungen zu entwickeln.

Falls es andere Erfahrungen oder Verbesserungsvorschläge gibt sind Pull Requests jederzeit willkommen!

## Docker

Verwendet wird das Standardimage `dvagcr.azurecr.io/bp/openjdk` bereitgestellt durch die Server Admins Linux. Das Verwenden von anderen Images, insbesondere derer aus dem "Internet", ist im Betriebskonzept nicht vorgesehen.

In dem Docker Image werden zwei Umgebungsvariablen definiert:

### JAVA_TOOL_OPTS

Hier können bspw. in einem Kubernetes Deployment die Konfigurationen der Java Runtime übergeben werden.

Zum Beispiel:

```yaml
env:
  - name: JAVA_TOOL_OPTS
    value: -Xms1G -Xmx1G
```

### SPRING_PROFILES_ACTIVE

Hier werden die aktiven Spring Profile definiert. So kann die umgebungspezifische Konfiguration immer direkt neben dem
Code liegen. Trotzdem bleibt das generierte Artefakt (der Docker Container) umgebungsunabhängig. Das aktive Profil
kann dann wieder durch das Kubernetes Deployment gesetzt werden:

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: entwicklung
```

## Kubernetes

### Deployment und Services

Im Deployment wird festgelegt, wie der Docker Container gestartet wird. Über das `command` mit seinen `args` werden einige Properties an die Java Anwendung übergeben, die von den Troubleshootern so angefordert wurden.

```yaml
command: ["/usr/bin/java"]
args:
  [
    "-Djava.awt.headless=true",
    "-Duser.home=/tmp",
    "-Dfile.encoding=UTF-8",
    "-Dsun.jnu.encoding=UTF-8",
    "-Duser.country=US",
    "-Duser.language=en",
    "-Djava.security.egd=file:/dev/./urandom",
    "-Dcom.sun.management.jmxremote",
    "-Dcom.sun.management.jmxremote.authenticate=false",
    "-Dcom.sun.management.jmxremote.ssl=false",
    "-Dcom.sun.management.jmxremote.local.only=false",
    "-Dcom.sun.management.jmxremote.port=1099",
    "-Dcom.sun.management.jmxremote.rmi.port=1099",
    "-Djava.rmi.server.hostname=127.0.0.1",
    "-jar",
    "/app.jar",
  ]
```

Im Deployment werden außerdem umgebungspezifische Konfigurationen wie die Java Options und das aktive Spring Profile aus der `ConfigMap` ausgelesen und als Umgebungsvariable des Containers konfiguriert.

```yaml
env:
  - name: JAVA_TOOL_OPTS
    valueFrom:
      configMapKeyRef:
        name: springbootonkubernetes-config
        key: environment.java_opts
  - name: SPRING_PROFILES_ACTIVE
    valueFrom:
      configMapKeyRef:
        name: springbootonkubernetes-config
        key: environment.spring_active_profiles
  - name: LOGGING_CONFIG
    value: classpath:logback-logstash.xml
```

### Ingress

**TODO**

### Readiness und Liveness probes

**TODO**

## Konfiguration

### Spring Configuration

Die Konfiguration der Anwendung wird über die
[Spring Configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html)
umgesetzt.
Allgemeine Konfigurationen kommen in die Datei `application.yml` (es können auch `.properties` Dateien verwendet werden.)
Umgebungspezifische Konfigurationen kommen in Dateien die dem Schema `application-<Umgebungsname>.yml` entsprechen. Es empfiehlt sich
für die lokale Entwicklungsumgebung den Namen `application-default.yml` zu verwenden. Dann muss das entsprechende Profil
beim Start der Anwendung aus der IDE nicht mit angegeben werden.

### Kubernetes Configmap

Für Umgebungsspezifische Konfigurationen werden Kubernetes Configmaps verwendet. Das hat den Vorteil, dass die Deployment
und Service Konfiguration für jede Umgebung verwendet werden kann.

In der ConfigMap werden auch die im Docker Container verwendeten Umgebungsvariablen gesetzt:

```yaml
data:
  environment.spring_active_profiles: entprod
  environment.java_opts: "-Xms1G -Xmx1G"
```

Eine Alternative zu diesem Verfahren sind Templating-Tools/Packetmanager wie [helm](https://helm.sh).

## Metriken

Metriken unterstützen Entwickler und Betrieb beim Betreiben der Anwendung. Im Kubernetes Cluster werden Metriken mit
Prometheus gesammelt und nach Zabbix exportiert. Gleichzeitig besteht für Entwickler die Möglichkeit sich eigene Dashboards
in Grafana anzulegen.

Mit Hilfe der Dashboards können die Entwickler Schwachstellen und Performancenengpässe im Code frühzeitig erkennen und
beheben.

### Micrometer

Für das Sammeln der Metriken in der Java Applikation wird [Micrometer](https://micrometer.io/) verwendet. Ab Spring Boot 2.0
ist Micrometer standardmäßig im Projekt enthalten. Es muss nur der Exporter für Prometheus hinzugefügt werden.

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

## Logging

Für das Logging wird Logback verwendet. Zusätzlich verwendet wir den `logstash-logback-encoder` um direkt im
Logstash-kompatiblem JSON-Format zu loggen. In der lokalen Entwicklungsumgebung ist der Encoder nicht aktiv
um den Entwicklern das Lesen der Logs.

Für das Logging im Kubernetes Cluster wird die config via Umgebungsvariable angezogen:

```yaml
env:
  - name: LOGGING_CONFIG
    value: classpath:logback-logstash.xml
```

Der Inhalt der Logback-Logstash Konfiguration:

```xml
<configuration debug="true">
    <appender name="console" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>user</includeMdcKeyName>
            <includeMdcKeyName>requestid</includeMdcKeyName>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="console"/>
    </root>
</configuration>
```

Über `includeMdcKeyName` werden die User und RequestId mit in den Logoutput aufgenommen.
Das Log Level kann weiterhin über die Spring Configuration erfolgen.

Es wird auschließlich in die Konsole geloggt. Das Logging in einer Datei ist nicht nötig und muss vermieden werden.

### Access Log

Damit Zugriffe auf die Service nachverfolgbar sind ist es außerdem gewünscht einen Access Log zur Verfügung zu stellen.
Am einfachsten geht dies mit der Bibliothek [Logback Access](https://logback.qos.ch/access.html).

Konfiguration im XML `logback-access.xml`:

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
          <pattern>%h %reqAttribute{requestid} %reqAttribute{user} [%t] "%r" %s "-" %b %D</pattern>
        </encoder>
    </appender>
    <appender-ref ref="CONSOLE"/>
</configuration>
```

Wird zusätzlich der passende Starter als Abhängigkeit definiert ist keine weitere Konfiguration nötig.

```xml
<dependency>
    <groupId>net.rakugakibox.spring.boot</groupId>
    <artifactId>logback-access-spring-boot-starter</artifactId>
    <version>2.7.1</version>
</dependency>
```

### Session Id & Request Id Filter

Die SessionId (ZOB) und die RequestId müssen aus dem Request ausgelesen werden. Das geht am besten über einen Filter. Die ausgelesenen Daten müssen sowohl im `MDC` von Logback als auch als Request-Attribute abgelegt werden, damit das Logging auf die Werte zugreifen kann.

_Beispiel RequestId Filter:_

```java
@Override
public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
  MDC.remove(REQUEST_ID);
  String requestId = servletRequest.getParameter(REQUEST_ID);
  if (requestId != null && requestId.length() > 0) {
    currentRequestAttributes().setAttribute(REQUEST_ID, requestId, SCOPE_REQUEST);
    MDC.put(REQUEST_ID, requestId);
  } else {
    Optional.ofNullable(servletRequest.getParameterMap())
        .orElseGet(Collections::emptyMap)
        .keySet()
        .stream()
        .filter(k -> k.startsWith("RID-")).findFirst()
        .ifPresent((rid) -> {
          currentRequestAttributes().setAttribute(REQUEST_ID, rid, SCOPE_REQUEST);
          MDC.put(REQUEST_ID, requestId);
        });
  }
  filterChain.doFilter(servletRequest, servletResponse);
}
```

In den SessionId Filter muss ebenfalls der Name des Users bzw. die Id des Users gespeichert werden, da diese für das Logging erforderlich sind.

## Graceful Shutdown

Kubernetes ermöglicht es unterbrechungsfreie Releases durchzuführen. Dabei werden nacheinander "alte" Pods terminiert
und "neue" Pods gestartet. Das funktioniert automatisch, wenn man ein Deployment verwendet.

Damit das Ganze ohne Fehler für die User oder andere Services abläuft ist es wichtig, dass der Pod/Container nicht sofort
runterfährt, wenn Kubernetes in anhalten möchte. Zunächst muss der Pod aus dem Loadbalancing genommen werden. Das passiert,
wenn die readiness Probe ein negatives Ergebnis liefert. Nachdem der Pod aus dem Loadbalancing raus ist, sollte ihm noch
eine gewisse Zeit (10-20Sekunden) gegeben werden um bereits erhaltene Requests abzuarbeiten. Je nach Antwortzeiten des
Services muss diese Zeit natürlich angepasst werden. Erst dann darf der Pod angehalten und ersetzt werden.

In dieser Beispielanwendung wird das mit Hilfe der Implementierung der
[Schweizer Bundesbahn umgesetzt](https://github.com/SchweizerischeBundesbahnen/springboot-graceful-shutdown). Hier liefert
ein Aufruf des `/health` Endpunkt ein anderes Ergebnis, sobald der Prozess einen `SIGTERM` erhält. Daher ist unsere readiness
Probe `/actuator/health`. Für die liveness Probe bietet sich beispielsweise der `/actuator/info` Endpunkt an. Wenn dieser
keine `2xx`er Antwort mehr liefert macht es vermutlich Sinn den Pod auszutauschen.

Die Konfiguration im Deployment sieht dann so aus:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/info
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 3
readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 3
```

Die Zeit, die zwischen `SIGTERM` und stoppen der JVM vergehen darf wird in der `application.yml` konfiguriert:

```yaml
estaGracefulShutdownWaitSeconds: 30
```

**Wichtig:** der Wert `periodSeconds` muss größer sein als der Wert `estaGracefulShutdownWaitSeconds`.

## "Externe Interne" Services

Services wie das ZOB können aus dem Kubernetes Cluster entweder über den DNS-Namen oder einen Kubernetes Services vom Typ
`External Name` aufgerufen werden.

## Jenkins Shared Library

Das Jenkinsfile nutzt die Jenkins Shared Library (aka JSL). Für die JSL gibt es einige Einstellungen, die aus den Helm-Values Files ausgelesen und verwendet werden (deployment:, template:, etc).
Siehe auch: https://projektportal.dvag.com/confluence/display/INT/Jenkins+Shared+Library
