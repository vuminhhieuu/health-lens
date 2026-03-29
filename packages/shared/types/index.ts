export interface User {
  id: string;
  email: string;
}

export interface Profile {
  id: string;
  userId: string;
  displayName: string;
}

export interface HealthRecord {
  id: string;
  profileId: string;
  examDate: string;
}
