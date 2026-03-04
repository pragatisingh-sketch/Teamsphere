export interface User {
    parent: any;
    id: any;
    firstName: string;
    lastName: string;
    ldap:string;
    startDate: string;
    team:string;
    newLevel:string;
    programManager:string;
    lead:string;
    vendor:string;
    email:string;
    status:string;
    lwdMlStartDate?:string | null;
    process:string;
    resignationDate:string;
    roleChangeEffectiveDate?:string | null;
    levelBeforeChange:string;
    levelAfterChange:string;
    lastBillingDate:string;
    backfillLdap:string;
    billingStartDate:string;
    language:string;
    tenureTillDate:string;
    //addedInGoVfsWhoMain:string;
   // addedInGoVfsWhoInactive:string;
    level:string
    profilePic:string
    inactiveReason:string
    pnseProgram:string
    location:string
    shift: string;
    inactive: boolean;
  }
  
  
  export interface LoginResponse {
    token: string;
    role: string;
    username:string;
    passwordChangeRequired: boolean;
  }