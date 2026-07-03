export type Role = "CUSTOMER" | "LAWYER" | "ADMIN";

export interface UserProfile {
  id: number;
  role: Role;
  name: string;
  email: string;
  phone: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: UserProfile;
}

export interface RegisterRequest {
  role: "CUSTOMER" | "LAWYER";
  name: string;
  email: string;
  phone?: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
