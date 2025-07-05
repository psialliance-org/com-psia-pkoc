import Foundation

struct KeyData: Codable
{
    let publicKey: Data
    let privateKey: Data
}

class KeyStore: Codable
{
    static var keys: KeyData?
    
    private static func fileURL() throws -> URL
    {
        try FileManager.default.url(for: .documentDirectory,
                                    in: .userDomainMask,
                                    appropriateFor: nil,
                                    create: false)
        
        .appendingPathComponent("com.laar.pkoc.data.store")
    }
    
    static func load(completion: @escaping (Result<KeyData, Error>) -> Void)
    {
        DispatchQueue.global(qos: .background).async
        {
            do
            {
                let fileURL = try fileURL()
                guard let file = try? FileHandle(forReadingFrom: fileURL) else
                {
                    print("WARNING: No stored data found")
                    DispatchQueue.main.async
                    {
                        completion(.failure(KeyStoreError.keyFileNotFound))
                    }
                    return
                }
                let data = try JSONDecoder().decode(KeyData.self, from: file.availableData)
                DispatchQueue.main.async
                {
                    completion(.success(data))
                }
            }
            catch
            {
                DispatchQueue.main.async
                {
                    completion(.failure(error))
                }
            }
        }
    }
    
    static func save(keyData: KeyData, completion: @escaping (Result<Bool, Error>) -> Void)
    {
        DispatchQueue.global(qos: .background).async
        {
            do
            {
                let data = try JSONEncoder().encode(keyData)
                let outFile = try fileURL()
                try data.write(to: outFile)
                DispatchQueue.main.async
                {
                    completion(.success(true))
                }
            }
            catch
            {
                DispatchQueue.main.async
                {
                    completion(.failure(error))
                }
            }
        }
    }
}


enum KeyStoreError: Error
{
    case keyFileNotFound
}
