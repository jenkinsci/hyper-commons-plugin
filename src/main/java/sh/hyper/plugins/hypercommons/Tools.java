/*
 * The MIT License
 *
 *  Copyright (c) 2016 HyperHQ Inc
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package sh.hyper.plugins.hypercommons;

import hudson.Extension;
import hudson.Plugin;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:xlgao@zju.edu.cn">Xianglin Gao</a>
 */

public class Tools extends Plugin implements Describable<Tools> {
    @Override
    public Descriptor<Tools> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<Tools> {
        private String hyperUrl;
        private String hyperAccessId;
        private String hyperSecretKey;
        private String dockerEmail;
        private String dockerUsername;
        private String dockerPassword;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            hyperUrl = formData.getString("hyperUrl");
            hyperAccessId = formData.getString("hyperAccessId");
            hyperSecretKey = formData.getString("hyperSecretKey");
            dockerEmail = formData.getString("dockerEmail");
            dockerUsername = formData.getString("dockerUsername");
            dockerPassword = formData.getString("dockerPassword");

            save();

            return super.configure(req, formData);
        }

        public String getHyperUrl() {
            return hyperUrl;
        }

        public String getHyperAccessId() {
            return hyperAccessId;
        }

        public String getHyperSecretKey() {
            return hyperSecretKey;
        }

        public String getDockerEmail() {
            return dockerEmail;
        }

        public String getDockerUsername() {
            return dockerUsername;
        }

        public String getDockerPassword() {
            return dockerPassword;
        }

        public boolean createTmpCredential() {
            try {
                String jsonStr;
                if (dockerEmail != null && dockerUsername != null && dockerPassword != null
                        && !dockerEmail.equals("") && !dockerUsername.equals("") && !dockerPassword.equals("")
                        ) {
                    String userNameAndPassword = dockerUsername + ":" + dockerPassword;
                    byte[] base64Byte = Base64.getEncoder().encode(userNameAndPassword.getBytes("UTF-8"));
                    String base64Str = new String(base64Byte, "UTF-8");

                    jsonStr = "{\"auths\": {" +
                            "\"https://index.docker.io/v1/\": {" +
                            "\"auth\": " + "\"" + base64Str + "\"," +
                            "\"email\": " + "\"" + dockerEmail + "\"" +
                            "}" +
                            "}," +
                            "\"clouds\": {" +
                            "\"" + hyperUrl + "\": {" +
                            "\"accesskey\": " + "\"" + hyperAccessId + "\"," +
                            "\"secretkey\": " + "\"" + hyperSecretKey + "\"" +
                            "}" +
                            "}" +
                            "}";
                } else {
                    jsonStr = "{\"auths\": {}," +
                            "\"clouds\": {" +
                            "\"" + hyperUrl + "\": {" +
                            "\"accesskey\": " + "\"" + hyperAccessId + "\"," +
                            "\"secretkey\": " + "\"" + hyperSecretKey + "\"" +
                            "}" +
                            "}" +
                            "}";
                }

                OutputStreamWriter jsonWriter = null;
                String configPath;

                File hyperPath = new File("/tmp/hyper-commons-plugin");
                try {
                    if (!hyperPath.exists()) {
                        if (!hyperPath.mkdir()) return false;
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                configPath = hyperPath.getPath() + "/config.json";

                File config = new File(configPath);
                if (!config.exists()) {
                    try {
                        if (!config.createNewFile()) return false;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    FileOutputStream configFile = new FileOutputStream(configPath);
                    jsonWriter = new OutputStreamWriter(configFile, "UTF-8");
                    jsonWriter.write(jsonStr);
                    jsonWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (jsonWriter != null) {
                            jsonWriter.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        public boolean removeTmpCredential() {
            File config = new File("/tmp/hyper-commons-plugin/config.json");
            File configPath = new File("/tmp/hyper-commons-plugin");
            return config.exists() && configPath.exists() && config.delete() && configPath.delete();
        }

        @Override
        public String getDisplayName() {
            return "Hyper_ Commons Plugin";
        }

        public FormValidation doTestConnection(@QueryParameter("hyperUrl") final String hyperUrl,
                                               @QueryParameter("hyperAccessId") final String hyperAccessId,
                                               @QueryParameter("hyperSecretKey") final String hyperSecretKey,
                                               @QueryParameter("dockerEmail") final String dockerEmail,
                                               @QueryParameter("dockerUsername") final String dockerUsername,
                                               @QueryParameter("dockerPassword") final String dockerPassword) throws IOException, ServletException {
            try {
                this.hyperUrl = hyperUrl;
                this.hyperAccessId = hyperAccessId;
                this.hyperSecretKey = hyperSecretKey;
                this.dockerEmail = dockerEmail;
                this.dockerUsername = dockerUsername;
                this.dockerPassword = dockerPassword;

                if (!createTmpCredential()) {
                    return FormValidation.ok("connection test failed!");
                }

                Process hypercli = null;
                try {
                    String jenkinsHome = Jenkins.getInstance().getRootDir().getPath();
                    String hyperCliPath = jenkinsHome + "/bin/hyper";
                    String configPath = "/tmp/hyper-commons-plugin";
                    String command = hyperCliPath + " --config " + configPath + " --host=" + this.hyperUrl + " info";
                    Runtime runtime = Runtime.getRuntime();
                    hypercli = runtime.exec(command);
                } catch (IOException e) {
                    removeTmpCredential();
                    e.printStackTrace();
                }

                if (hypercli == null) {
                    removeTmpCredential();
                    return FormValidation.ok("connection test failed!");
                } else {
                    hypercli.waitFor(10, TimeUnit.SECONDS);
                }

                if (!removeTmpCredential()) {
                    return FormValidation.ok("connection test failed!");
                }
                
                if (hypercli.exitValue() == 0) {
                    return FormValidation.ok("connection test succeeded!");
                } else {
                    return FormValidation.ok("connection test failed!");
                }
            } catch (Exception e) {
                removeTmpCredential();
                return FormValidation.error("connection test error : " + e.getMessage());
            }
        }

        //download Hypercli
        public FormValidation doDownloadHypercli() throws IOException, ServletException {
            try {
                String urlPath = "https://mirror-hyper-install.s3.amazonaws.com/hyper";
                String hyperCliPath;
                URL url = new URL(urlPath);
                URLConnection connection = url.openConnection();
                InputStream in = connection.getInputStream();
                FileOutputStream os = null;

                File jenkinsHome = Jenkins.getInstance().getRootDir();

                File hyperPath = new File(jenkinsHome.getPath() + "/bin");
                try {
                    if (!hyperPath.exists()) {
                        if (!hyperPath.mkdir()) return FormValidation.ok("downloading hypercli failed!");
                    }
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
                hyperCliPath = jenkinsHome.getPath() + "/bin/hyper";

                try {
                    os = new FileOutputStream(hyperCliPath);
                    byte[] buffer = new byte[4 * 1024];
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        os.write(buffer, 0, read);
                    }
                    os.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (os != null) {
                            os.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                in.close();

                try {
                    String command = "chmod +x " + hyperCliPath;
                    Runtime runtime = Runtime.getRuntime();
                    runtime.exec(command);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return FormValidation.ok("Hypercli downloaded!");
            } catch (Exception e) {
                return FormValidation.error("Downloading Hypercli error : " + e.getMessage());
            }
        }
    }
}
