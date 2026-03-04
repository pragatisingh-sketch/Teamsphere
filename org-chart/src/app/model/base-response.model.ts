// Interface for the backend BaseResponse structure
export interface BaseResponse<T> {
    status: string;
    code: number;
    message: string;
    data: T;
}
