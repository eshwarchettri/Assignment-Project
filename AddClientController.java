@RestController
@Configuration
@PropertySource(value = {"classpath:application.properties"})
@RequestMapping("/user")
public class AddClientController {

    private static final Log LOGGER = LogFactory.getLog(AddClientController.class);
    @Value("${gmail.client.clientId}")
    private String CLIENT_ID;

    @Value("${gmail.client.redirectUri}")
    private String REDIRECT_URI;

    @Value("${gmail.client.clientSecret}")
    private String CLIENT_SECRET;

    GoogleClientSecrets clientSecrets;
    GoogleAuthorizationCodeFlow flow;
    Credential credential;
    private static HttpTransport httpTransport;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @Autowired
    private UserService userService;

    @Autowired
    GmailService gmailService;

    @Autowired
    ClientInfoService clientInformationService;

    @RequestMapping(value = "/newUser", method = RequestMethod.POST)
    public ResponseEntity addNewClient(HttpServletRequest request, @RequestBody HashMap<String, String> mapper, HttpSession session, HttpServletResponse response) {
        String username = mapper.get("username");
        String password = mapper.get("password");
        String email = mapper.get("email");
        if (userService.findByUsername(username) != null) {
            return new ResponseEntity("usernameExists", HttpStatus.BAD_REQUEST);
        }

        if (userService.findByEmail(email) != null) {
            return new ResponseEntity("emailExists", HttpStatus.BAD_REQUEST);
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(SecurityUtility.passwordEncoder().encode(password));

        Role role = new Role();
        role.setRoleId(1);
        role.setName("ROLE_USER");
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(new UserRole(user, role));
        User newUser = userService.createUser(user, userRoles);
        RedirectView redirectView = new RedirectView();
        if (userService.findByUsername(username) != null) {
            try {
                session.setAttribute("clientId", newUser.getId());
                session.setAttribute("clientEmailId", newUser.getEmail());
                redirectView = googleConnectionStatus(response);
                LOGGER.info(redirectView.getUrl());
              response.sendRedirect(redirectView.getUrl());    
             return null;           
            } catch (Exception ex) {
                Logger.getLogger(AddClientController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return new ResponseEntity(redirectView.getUrl(), HttpStatus.OK);
    }

    public RedirectView googleConnectionStatus(HttpServletResponse response) throws Exception {
             return new RedirectView(authorize());
    }

    private String authorize() throws Exception {
        AuthorizationCodeRequestUrl authorizationUrl;
        if (flow == null) {
            GoogleClientSecrets.Details web = new GoogleClientSecrets.Details();
            web.setClientId(CLIENT_ID);
            web.setClientSecret(CLIENT_SECRET);
            clientSecrets = new GoogleClientSecrets().setWeb(web);
            httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
                    Collections.singleton(GmailScopes.GMAIL_READONLY)).build();
        }
        authorizationUrl = flow.newAuthorizationUrl().setRedirectUri(REDIRECT_URI).setAccessType("offline").setApprovalPrompt("force");

        System.out.println("gamil authorizationUrl ->" + authorizationUrl);
        return authorizationUrl.build();
    }

    @RequestMapping(value = "/login/gmailCallback", method = RequestMethod.GET, params = "code")
    public ResponseEntity<Map<String, ArrayList<String>>> oauth2Callback(@RequestParam(value = "code") String code, HttpSession session, Model model) {
        Map<String, ArrayList<String>> headerColumns = new LinkedHashMap<>();
        try {
            TokenResponse response = flow.newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
            credential = flow.createAndStoreCredential(response, "userID");
            Integer clientId = (Integer) session.getAttribute("clientId");
            clientInformationService.saveTokens(response, clientId);
            ListMessagesResponse MsgResponse = gmailService.getGmailMessage(credential, clientId);
            headerColumns = gmailService.readMessgeAndProcess(session, MsgResponse, false, clientId);
        } catch (IOException e) {

            LOGGER.debug("error is due to: " + e);

        }
        session.setAttribute("headerColumns", headerColumns);
        model.addAttribute("headerColumns", headerColumns);
        model.addAttribute("clientId", session.getAttribute("clientId"));
        model.addAttribute("emailId", session.getAttribute("clientEmailId"));
        model.addAttribute("mapHeaders", new HeaderMappingDetails());
        return new ResponseEntity(headerColumns, HttpStatus.OK);
    }

}