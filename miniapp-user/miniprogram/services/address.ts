import type { AddressCreateDTO, AddressUpdateDTO, AddressVO, DisableRequestDTO } from '@fixmate/api-contract';
import { request } from '../utils/request';

export function listAddresses(): Promise<AddressVO[]> {
  return request<AddressVO[]>({ method: 'GET', path: '/api/v1/user/addresses' });
}

export function createAddress(dto: AddressCreateDTO): Promise<AddressVO> {
  return request<AddressVO>({
    method: 'POST',
    path: '/api/v1/user/addresses',
    data: dto
  });
}

export function updateAddress(id: string, dto: AddressUpdateDTO): Promise<AddressVO> {
  return request<AddressVO>({
    method: 'PUT',
    path: '/api/v1/user/addresses/' + id,
    data: dto
  });
}

export function disableAddress(id: string, dto: DisableRequestDTO): Promise<void> {
  return request<void>({
    method: 'DELETE',
    path: '/api/v1/user/addresses/' + id,
    data: dto
  });
}
